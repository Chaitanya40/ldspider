package com.ontologycentral.ldspider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.util.CallbackNxOutputStream;

import com.ontologycentral.ldspider.frontier.Frontier;
import com.ontologycentral.ldspider.frontier.RankedFrontier;
import com.ontologycentral.ldspider.hooks.error.ErrorHandler;
import com.ontologycentral.ldspider.hooks.error.ErrorHandlerLogger;
import com.ontologycentral.ldspider.hooks.error.ObjectThrowable;
import com.ontologycentral.ldspider.hooks.fetch.FetchFilterRdfXml;
import com.ontologycentral.ldspider.hooks.fetch.FetchFilterSuffix;
import com.ontologycentral.ldspider.hooks.links.LinkFilter;
import com.ontologycentral.ldspider.hooks.links.LinkFilterDefault;
import com.ontologycentral.ldspider.hooks.links.LinkFilterDomain;
import com.ontologycentral.ldspider.hooks.links.LinkFilterDummy;
import com.ontologycentral.ldspider.hooks.sink.Sink;
import com.ontologycentral.ldspider.hooks.sink.SinkCallback;
import com.ontologycentral.ldspider.hooks.sink.SinkSparul;

public class Main{
	private final static Logger _log = Logger.getLogger(Main.class.getSimpleName());

	public static void main(String[] args) {
		Options options = new Options();

		OptionGroup input = new OptionGroup();

		Option seeds = OptionBuilder.withArgName("file")
		.hasArgs(1)
		.withDescription("location of seed list")
		.create("s");
		seeds.setRequired(true);
		input.addOption(seeds);

		Option uri = OptionBuilder.withArgName("uri")
		.hasArgs(1)
		.withDescription("uri of an instance")
		.create("u");
		uri.setRequired(true);
		input.addOption(uri);
		options.addOptionGroup(input);

		//Strategy
		OptionGroup strategy = new OptionGroup();

		/*
		Option ondisk = OptionBuilder.withArgName("directory max-uris")
		.hasArgs(1)
		.withDescription("use on-disk queue with URI selection based on frequency")
		.create("d");
		strategy.addOption(ondisk);
		*/
//		Option simple = new Option("a", false, "just fetch URIs from list");
//		strategy.addOption(simple);

		Option bfs = new Option("b", false, "do strict breadth-first (uri-limit and pld-limit optional)");
		bfs.setArgs(3);
		bfs.setArgName("depth uri-limit pld-limit");
		strategy.addOption(bfs);

		Option opti = new Option("c", false, "use load balanced crawling strategy");
		opti.setArgs(1);
		opti.setArgName("max-uris");
		strategy.addOption(opti);
		
		options.addOptionGroup(strategy);
		
		Option threads = OptionBuilder.withArgName("threads")
		.hasArgs(1)
		.withDescription("number of threads (default "+CrawlerConstants.DEFAULT_NB_THREADS+")")
		.create("t");
		options.addOption(threads);

		Option stay = OptionBuilder.withArgName("stay")
		.hasArgs(0)
		.withDescription("stay on hostnames of seed uris")
		.create("y");
		options.addOption(stay);

		//Redirects
		Option redirs = OptionBuilder.withArgName("redirects")
		.hasArgs(1)
		.withDescription("write redirects.nx file")
		.create("r");
		options.addOption(redirs);

		Option noLinks = new Option("n", false, "do not extract links - just follow redirects");
		noLinks.setArgs(0);
		options.addOption(noLinks);
		
		//Output
		OptionGroup output = new OptionGroup();
		
		Option outputFile = OptionBuilder.withArgName("file")
		.hasArgs(1)
		.withDescription("name of NQuad file with output")
		.create("o");
		output.addOption(outputFile);
		
		Option outputEndpoint = OptionBuilder.withArgName("uri")
		.hasArgs(1)
		.withDescription("URI of a Triple Store endpoint supporting SPARQL/Update")
		.create("oe");
		output.addOption(outputEndpoint);
		
		options.addOptionGroup(output);

		//Logging
		Option log = OptionBuilder.withArgName("file")
		.hasArgs(1)
		.withDescription("name of access log file")
		.create("a");
		options.addOption(log);
		
		Option helpO = new Option("h", "help", false, "print help");
		options.addOption(helpO);

		CommandLineParser parser = new BasicParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args,true);
			if (cmd.hasOption("h") || cmd.hasOption("help")) {
				formatter.printHelp(80," ","Crawling and lookups on the linked data web\n", options,"\nFeedback and comments are welcome",true );
				System.exit(0);
			} else if (!cmd.hasOption("s") && !cmd.hasOption("u")) {
				formatter.printHelp(80," ","ERROR: Missing required option: s or u \n", options,"\nError occured! Please see the error message above",true );
				System.exit(-1);    
			}

			run(cmd);
		} catch (org.apache.commons.cli.ParseException e) {
			formatter.printHelp(80," ","ERROR: "+e.getMessage()+"\n", options,"\nError occured! Please see the error message above",true );
			System.exit(-1);
		} catch (FileNotFoundException e) {
			formatter.printHelp(80," ","ERROR: "+e.getMessage()+"\n", options,"\nError occured! Please see the error message above",true );
			System.exit(-1);
		} catch (NumberFormatException e) {
			formatter.printHelp(80," ","ERROR: "+e.getMessage()+"\n", options,"\nError occured! Please see the error message above",true );
			System.exit(-1);
		}
	}

	private static void run(CommandLine cmd) throws FileNotFoundException {
		// check seed file
		Set<URI> seeds = null;
		if (cmd.hasOption("s")) {
			File seedList = new File(cmd.getOptionValue("s"));
			if (!seedList.exists()) {
				throw new FileNotFoundException("No file found at "+seedList.getAbsolutePath());
			}
			seeds = readSeeds(seedList);
		} else if (cmd.hasOption("u")) {
			seeds = new HashSet<URI>();
			try {
				seeds.add(new URL(cmd.getOptionValue("u").trim()).toURI());
			} catch (Exception e) {
				_log.warning("Discard invalid uri "+e.getMessage()+" for "+cmd.hasOption("u"));
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		_log.info("no of seed uris " + seeds.size());
		
		Sink sink;
		OutputStream os = null;
		if (cmd.hasOption("o")) {
			os = new FileOutputStream(cmd.getOptionValue("o"));
			sink = new SinkCallback(new CallbackNxOutputStream(os));
		}
		else if(cmd.hasOption("oe")) {
			sink = new SinkSparul(cmd.getOptionValue("oe"), true);
		}
		else {
			sink = new SinkCallback(new CallbackNxOutputStream(System.out));
		}
				
		PrintStream ps = System.out;
		if (cmd.hasOption("a")) {
			ps = new PrintStream(new FileOutputStream(cmd.getOptionValue("a")));			
		}
		
		Callback rcb = null;
		if (cmd.hasOption("r")) {
			FileOutputStream fos = new FileOutputStream(cmd.getOptionValue("r"));
			rcb = new CallbackNxOutputStream(fos);
			rcb.startDocument();
		}
	
		ErrorHandler eh = new ErrorHandlerLogger(ps, rcb);
		
		Frontier frontier = new RankedFrontier();
		frontier.setErrorHandler(eh);
		frontier.addAll(seeds);
		
		_log.info("frontier done");

		LinkFilter links = null;
		
		if (cmd.hasOption("y")) {
			LinkFilterDomain lfd = new LinkFilterDomain(frontier);	
			for (URI u : seeds) {
				lfd.addHost(u.getHost());
			}
			links = lfd;
		} else if (cmd.hasOption("n")) {
			LinkFilterDummy d = new LinkFilterDummy();
			links = d;
		} else {
			links = new LinkFilterDefault(frontier);	
		}
		
		links.setErrorHandler(eh);
		
		int threads = CrawlerConstants.DEFAULT_NB_THREADS;

		if (cmd.hasOption("t")) {
			threads = Integer.valueOf(cmd.getOptionValue("t"));
		}

		long time = System.currentTimeMillis();

		FetchFilterRdfXml ffrdf = new FetchFilterRdfXml();
		ffrdf.setErrorHandler(eh);
		
		FetchFilterSuffix blacklist = new FetchFilterSuffix(CrawlerConstants.BLACKLIST);

		_log.info("init crawler");

		Crawler c = new Crawler(threads);
		c.setErrorHandler(eh);
		c.setOutputCallback(sink);
		c.setLinkFilter(links);
		c.setFetchFilter(ffrdf);
		c.setBlacklistFilter(blacklist);
		
		if (cmd.hasOption("b")) {
			String[] vals = cmd.getOptionValues("b");
			
			int depth = Integer.parseInt(vals[0]);
			int maxuris = -1;
			int maxplds = -1;

			if (vals.length > 1) {
				maxuris = Integer.parseInt(vals[1]);
				if (vals.length > 2) {
					maxplds = Integer.parseInt(vals[2]);
				}
			}

			_log.info("breadth-first crawl with " + threads + " threads, depth " + depth + " maxuris " + maxuris + " maxplds " + maxplds);
			
			c.evaluateBreadthFirst(frontier, depth, maxuris, maxplds);
		} else if (cmd.hasOption("c")) {
			int maxuris = Integer.parseInt(cmd.getOptionValues("c")[0]);

			_log.info("load balanced crawl with " + threads + " threads, maxuris " + maxuris);

			c.evaluateLoadBalanced(frontier, maxuris);
		}
	
		for (Iterator<ObjectThrowable> it = eh.iterator(); it.hasNext() ; ) {
			ObjectThrowable ot = it.next();
			System.err.println(ot.getThrowable().getMessage() + " " + ot.getObject());
		}

		System.err.println(eh);

		c.close();

		long time1 = System.currentTimeMillis();
		
		if(os != null) {
			try {
				os.close();
			} catch (IOException e) {
				_log.warning("could not close output stream: " + e.getMessage());
			}
		}
		
		if (rcb != null) {
			rcb.endDocument();
		}

		System.err.println("time elapsed " + (time1-time) + " ms " + (float)eh.lookups()/((time1-time)/1000.0) + " lookups/sec");
	}

	/**
	 * 
	 * @param q - queue
	 * @param seedList
	 * @throws FileNotFoundException - should never happen since the check was done in method before
	 */
	static Set<URI> readSeeds(File seedList) throws FileNotFoundException {
		Set<URI> seeds = new HashSet<URI>();
		
		BufferedReader br = new BufferedReader(new FileReader(seedList));
		
		String line = null;
		URL uri = null;
		int i = 0;
		
		try {
			while ((line = br.readLine()) != null) {
				i++;
				if (line != null) {
					line = line.trim();
					try {
						uri = new URL(line);
						seeds.add(uri.toURI());	
					} catch (URISyntaxException e) {
						_log.fine("Discard invalid uri " + e.getMessage() + " for " + line);
					} catch (MalformedURLException e) {
						_log.fine("Discard invalid uri " + e.getMessage() + " for " + line);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			_log.fine(e.getMessage());
		}
		
		_log.info("read " + i + " lines from seed file");
		
		return seeds;
	}
}
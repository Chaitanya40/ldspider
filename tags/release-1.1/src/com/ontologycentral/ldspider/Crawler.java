package com.ontologycentral.ldspider;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.semanticweb.yars.nx.parser.Callback;

import com.ontologycentral.ldspider.frontier.Frontier;
import com.ontologycentral.ldspider.hooks.content.ContentHandler;
import com.ontologycentral.ldspider.hooks.content.ContentHandlerRdfXml;
import com.ontologycentral.ldspider.hooks.error.ErrorHandler;
import com.ontologycentral.ldspider.hooks.error.ErrorHandlerDummy;
import com.ontologycentral.ldspider.hooks.fetch.FetchFilter;
import com.ontologycentral.ldspider.hooks.fetch.FetchFilterAllow;
import com.ontologycentral.ldspider.hooks.links.LinkFilter;
import com.ontologycentral.ldspider.hooks.links.LinkFilterDefault;
import com.ontologycentral.ldspider.hooks.sink.Sink;
import com.ontologycentral.ldspider.hooks.sink.SinkCallback;
import com.ontologycentral.ldspider.hooks.sink.SinkDummy;
import com.ontologycentral.ldspider.http.ConnectionManager;
import com.ontologycentral.ldspider.http.LookupThread;
import com.ontologycentral.ldspider.http.robot.Robots;
import com.ontologycentral.ldspider.queue.BreadthFirstQueue;
import com.ontologycentral.ldspider.queue.LoadBalancingQueue;
import com.ontologycentral.ldspider.queue.Redirects;
import com.ontologycentral.ldspider.queue.SpiderQueue;
import com.ontologycentral.ldspider.tld.TldManager;

public class Crawler {
	Logger _log = Logger.getLogger(this.getClass().getName());

	ContentHandler _contentHandler;
	Sink _output;
	LinkFilter _links;
	ErrorHandler _eh;
	FetchFilter _ff, _blacklist;
	ConnectionManager _cm;
	
	Robots _robots;
//	Sitemaps _sitemaps;
	
	TldManager _tldm;

	SpiderQueue _queue = null;
	
	int _threads;
	
	/**
	 * The Crawling mode.
	 * Defines whether ABox and/or TBox links are followed and whether an extra TBox round is done.
	 */
	public enum Mode
	{
		/** Only crawl ABox statements */
		ABOX_ONLY(true, false, false),
		/** Only crawl TBox statements */
		TBOX_ONLY(false, true, false),
		/** Crawl ABox and TBox statements */
		ABOX_AND_TBOX(true, true, false),
		/** Crawl ABox and TBox statements and do an extra round to get the TBox of the statements retrieved in the final round */
		ABOX_AND_TBOX_EXTRAROUND(true, true, true);
		
		private boolean aBox;
		private boolean tBox;
		private boolean extraRound;
	
	    private Mode(boolean aBox, boolean tBox, boolean extraRound) {
	    	this.aBox = aBox;
	    	this.tBox = tBox;
	    	this.extraRound = extraRound;
		}

		public boolean followABox() {
			return aBox;
		}

		public boolean followTBox() {
			return tBox;
		}

		public boolean doExtraRound() {
			return extraRound;
		}
	}
	
	public Crawler() {
		this(CrawlerConstants.DEFAULT_NB_THREADS);
	}
	public Crawler(int threads) {
		this(threads,null,null,null,null);
	}
	
	/**
	 * 
	 * @param threads
	 * @param proxyHost - the proxy host or <code>null</code> to use System.getProperties().get("http.proxyHost")
	 * @param proxyPort - the proxy port or <code>null</code> to use System.getProperties().get("http.proxyPort")
	*/
	public Crawler(int threads,String proxyHost, String proxyPort){
		this(threads,proxyHost,proxyPort,null,null);
	}
	
	/**
	 * 
	 * @param threads
	 * @param proxyHost - the proxy host or <code>null</code> to use System.getProperties().get("http.proxyHost")
	 * @param proxyPort - the proxy port or <code>null</code> to use System.getProperties().get("http.proxyPort")
	 * @param proxyUser - the proxy user or <code>null</code> to use System.getProperties().get("http.proxyUser")
	 * @param proxyPassword - the proxy user password or <code>null</code> to use System.getProperties().get("http.proxyPassword")
	 */
	public Crawler(int threads,String proxyHost, String proxyPort, String proxyUser, String proxyPassword) {
		_threads = threads;
		
		String phost = proxyHost;
		int pport = 0;
		if(proxyPort!=null){
			try{
				pport = Integer.parseInt(proxyPort);
			}catch(NumberFormatException nfe){
				pport = 0;
			}
		}
		String puser = proxyUser;
		String ppassword = proxyPassword;
		
		
		if (phost == null && System.getProperties().get("http.proxyHost") != null) {
			phost = System.getProperties().get("http.proxyHost").toString();
		}
		if (pport==0 && System.getProperties().get("http.proxyPort") != null) {
			pport = Integer.parseInt(System.getProperties().get("http.proxyPort").toString());
		}
		
		if (puser == null && System.getProperties().get("http.proxyUser") != null) {
			puser = System.getProperties().get("http.proxyUser").toString();
		}
		if (ppassword == null && System.getProperties().get("http.proxyPassword") != null) {
			ppassword = System.getProperties().get("http.proxyPassword").toString();
		}
		
	    _cm = new ConnectionManager(phost, pport, puser, ppassword, threads*CrawlerConstants.MAX_CONNECTIONS_PER_THREAD);
	    _cm.setRetries(CrawlerConstants.RETRIES);
	    
	    try { 
		    _tldm = new TldManager(_cm);
		} catch (Exception e) {
			_log.info("cannot get tld file online " + e.getMessage());
			try {
				_tldm = new TldManager();
			} catch (IOException e1) {
				_log.info("cannot get tld file locally " + e.getMessage());
			}
		}

		_eh = new ErrorHandlerDummy();

	    _robots = new Robots(_cm);
	    _robots.setErrorHandler(_eh);
	    
//	    _sitemaps = new Sitemaps(_cm);
//	    _sitemaps.setErrorHandler(_eh);
		
	    _contentHandler = new ContentHandlerRdfXml();
	    _output = new SinkDummy();
		_ff = new FetchFilterAllow();
		
		_blacklist = new FetchFilterAllow();
	}
	
	public void setContentHandler(ContentHandler h) {
		_contentHandler = h;
	}
	
	public void setFetchFilter(FetchFilter ff) {
		_ff = ff;
	}

	public void setBlacklistFilter(FetchFilter blacklist) {
		_blacklist = blacklist;
	}

	public void setErrorHandler(ErrorHandler eh) {
		_eh = eh;
		
		if (_robots != null) {
			_robots.setErrorHandler(eh);
		}
//		if (_sitemaps != null) {
//			_sitemaps.setErrorHandler(eh);
//		}
		if (_links != null) {
			_links.setErrorHandler(eh);
		}
	}
	
	public void setOutputCallback(Callback cb) {
		_output = new SinkCallback(cb);
	}
	
	public void setOutputCallback(Sink sink) {
		_output = sink;
	}
	
	public void setLinkFilter(LinkFilter links) {
		_links = links;
	}
	
	public void evaluateBreadthFirst(Frontier frontier, int depth, int maxuris, int maxplds) {
		evaluateBreadthFirst(frontier, depth, maxuris, maxplds, Mode.ABOX_AND_TBOX);
	}
	
	public void evaluateBreadthFirst(Frontier frontier, int depth, int maxuris, int maxplds, Mode crawlingMode) {
		if (_queue == null || !(_queue instanceof BreadthFirstQueue)) {
			_queue = new BreadthFirstQueue(_tldm, maxuris, maxplds);
		} else {
			Redirects r = _queue.getRedirects();
			Set<URI> seen = _queue.getSeen();
			_queue = new BreadthFirstQueue(_tldm, maxuris, maxplds);
			_queue.setRedirects(r);
			_queue.setSeen(seen);
		}
		
		if (_links == null) {
			_links = new LinkFilterDefault(frontier);
		}
		
		_queue.schedule(frontier);
		
		_links.setFollowABox(crawlingMode.followABox());
		_links.setFollowTBox(crawlingMode.followTBox());
		
		_log.info(_queue.toString());
		
		int rounds = crawlingMode.doExtraRound() ? depth + 1 : depth;
		for (int curRound = 0; curRound <= rounds; curRound++) {
			List<Thread> ts = new ArrayList<Thread>();
			
			//Extra round to get TBox
			if(curRound == depth + 1) {
				_links.setFollowABox(false);
				_links.setFollowTBox(true);
			}

			for (int j = 0; j < _threads; j++) {
				LookupThread lt = new LookupThread(_cm, _queue, _contentHandler, _output, _links, _robots, _eh, _ff, _blacklist);
				ts.add(new Thread(lt,"LookupThread-"+j));		
			}

			_log.info("Starting threads round " + curRound + " with " + _queue.size() + " uris");
			
			for (Thread t : ts) {
				t.start();
			}

			for (Thread t : ts) {
				try {
					t.join();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			
			_log.info("ROUND " + curRound + " DONE with " + _queue.size() + " uris remaining in queue");
			_log.fine("old queue: \n" + _queue.toString());

			_queue.schedule(frontier);

			_log.fine("new queue: \n" + _queue.toString());
		}
	}
	
	public void evaluateLoadBalanced(Frontier frontier, int maxuris) {
		if (_queue == null || !(_queue instanceof LoadBalancingQueue)) {
			_queue = new LoadBalancingQueue(_tldm);
		} else {
			Redirects r = _queue.getRedirects();
			Set<URI> seen = _queue.getSeen();
			_queue = new LoadBalancingQueue(_tldm);
			_queue.setRedirects(r);
			_queue.setSeen(seen);
		}

		if (_links == null) {
			_links = new LinkFilterDefault(frontier);
		}
		
		_queue.schedule(frontier);
		
		_log.fine(_queue.toString());

		int i = 0;
		int uris = 0;
		
		while (uris < maxuris && _queue.size() > 0) {
			int size = _queue.size();
			
			List<Thread> ts = new ArrayList<Thread>();

			for (int j = 0; j < _threads; j++) {
				LookupThread lt = new LookupThread(_cm, _queue, _contentHandler, _output, _links, _robots, _eh, _ff, _blacklist);
				ts.add(new Thread(lt,"LookupThread-"+j));		
			}

			_log.info("Starting threads round " + i++ + " with " + _queue.size() + " uris");
			
			for (Thread t : ts) {
				t.start();
			}

			for (Thread t : ts) {
				try {
					t.join();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			
			uris += size - _queue.size();
			
			_log.info("ROUND " + i + " DONE with " + _queue.size() + " uris remaining in queue");
			_log.fine("old queue: \n" + _queue.toString());

			_log.fine("frontier" + frontier);
			
			_queue.schedule(frontier);

			_log.info("new queue: \n" + _queue.toString());
		}
	}
	
	
	
	public void run(SpiderQueue queue){
		List<Thread> ts = new ArrayList<Thread>();

		for (int j = 0; j < _threads; j++) {
			LookupThread lt = new LookupThread(_cm, queue, _contentHandler, _output, _links, _robots, _eh, _ff, _blacklist);
			ts.add(new Thread(lt,"LookupThread-"+j));		
		}

		
		for (Thread t : ts) {
			t.start();
		}

		for (Thread t : ts) {
			try {
				t.join();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * Set the spider queue
	 * @param queue
	 */
	public void setQueue(final SpiderQueue queue){
		_queue = queue;
	}
	/**
	 * 
	 * @return - the current used {@link SpiderQueue}
	 */
	public SpiderQueue getQueue(){
		return _queue;
	}
	
	public TldManager getTldManager(){
		return _tldm;
	}
	public void close() {
		_cm.shutdown();
		_eh.close();
	}
}

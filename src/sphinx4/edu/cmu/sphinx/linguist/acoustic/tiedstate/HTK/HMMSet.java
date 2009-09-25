/*
 * Copyright 2007 LORIA, France.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.linguist.acoustic.tiedstate.HTK;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * 
 * @author cerisara
 */
public class HMMSet {
	private GMMDiag g;
	private int ngauss;
	float[][] trans;
	/**
	 * contains HMMState instances
	 */
	public List<HMMState> etats;
	public List<float[][]> transitions = new ArrayList<float[][]>();
	public Map<String,Integer> transNames = new HashMap<String,Integer>();
	
	public Iterator<SingleHMM> get1phIt() {
		Iterator<SingleHMM> it = new Iterator<SingleHMM>() {
			int cur;
			public void remove() {
			}
			public SingleHMM next() {
				for (;;) {
					if (cur>=hmms.size()) return null;
					SingleHMM hmm = hmms.get(cur++);
					if (hmm.getNom().indexOf('-')>=0||hmm.getNom().indexOf('+')>=0)
						continue;
					return hmm;
				}
			}
			public boolean hasNext() {
				return false;
			}
		};
		return it;
	}
	public Iterator<SingleHMM> get3phIt() {
		Iterator<SingleHMM> it = new Iterator<SingleHMM>() {
			int cur;
			public void remove() {
			}
			public SingleHMM next() {
				for (;;) {
					if (cur>=hmms.size()) return null;
					SingleHMM hmm = hmms.get(cur++);
					if (!(hmm.getNom().indexOf('-')>=0||hmm.getNom().indexOf('+')>=0))
						continue;
					return hmm;
				}
			}
			public boolean hasNext() {
				return false;
			}
		};
		return it;
	}
	public int getStateIdx(HMMState st) {
		return st.gmmidx;
	}
	public int getHMMidx(SingleHMM hmm) {
		for (int i=0;i<hmms.size();i++) {
			SingleHMM h = hmms.get(i);
			if (h==hmm) return i;
		}
		return -1;
	}
	public int getNstates() {
		return gmms.size();
	}
	public String [] getHMMnames() {
		String [] rep = new String[hmms.size()];
		for (int i=0;i<rep.length;i++) {
			SingleHMM h = hmms.get(i);
			rep[i]=h.getNom();
		}
		return rep;
	}
	/**
	 * contains GMMDiag instances
	 */
	public List<GMMDiag> gmms;
	/**
	 * contains HMM instances
	 */
	public List<SingleHMM> hmms;
	public int getNhmms() {
		return hmms.size();
	}
	public int getNhmmsMono() {
		int n=0;
        for (SingleHMM hmm : hmms) {
            if (!(hmm.getNom().indexOf('-') >= 0 || hmm.getNom().indexOf('+') >= 0))
                n++;
        }
		return n;
	}
	public int getNhmmsTri() {
		int n=0;
        for (SingleHMM hmm : hmms) {
            if (hmm.getNom().indexOf('-') >= 0 || hmm.getNom().indexOf('+') >= 0)
                n++;
        }
		return n;
	}
	public int getHMMIndex(SingleHMM h) {
		return hmms.indexOf(h);
	}
	/**
	 * 
	 * @param hmmidx index of the HMM (begins at 0)
	 * @param stateidx index of the state WITHIN the HMM ! (begins at 1, as in MMF)
	 * @return index of the state in the vector of all the states of the HMMSet !
	 */
	public int getStateIdx(int hmmidx, int stateidx) {
		// TODO: stocker un tableau de correspondance pour ne pas recalculer a chaque fois !
		SingleHMM hmm;
		int nEmittingStates = 0;
		for (int i=0;i<hmmidx;i++) {
			hmm= hmms.get(i);
			nEmittingStates += hmm.getNbEmittingStates();
		}
		hmm= hmms.get(hmmidx);
		for (int i=1;i<stateidx;i++) {
			if (hmm.isEmitting(i)) nEmittingStates++;
		}
		if (hmm.isEmitting(stateidx))
			return nEmittingStates; // pas de +1 car on compte les etats a partir de 0
		else return -1;
	}
	public SingleHMM getHMM(int idx) {
		return hmms.get(idx);
	}
	public SingleHMM getHMM(String nom) {
		SingleHMM h=null;
        for (SingleHMM hmm : hmms) {
            h = hmm;
            if (h.getNom().equals(nom)) break;
        }
		return h;
	}
	
	public HMMSet() {
		etats = new ArrayList<HMMState>();
		hmms = new ArrayList<SingleHMM>();
		gmms = new ArrayList<GMMDiag>();
	}
	
	public void loadHTK(String nomFich) {
		try {
		    BufferedReader f = new BufferedReader(new FileReader(nomFich));
		    String s;
			for (;;) {
				s = f.readLine();
				if (s == null)
					break;
				if (s.startsWith("~s")) {
					String nomEtat = s.substring(s.indexOf('"') + 1, s.lastIndexOf('"'));
					loadEtat(f, nomEtat, null);
				} else if (s.startsWith("~v")) {
					// variance floor: bypass
				} else if (s.startsWith("~t")) {
					String nomTrans = s.substring(s.indexOf('"') + 1, s.lastIndexOf('"'));
					loadTrans(f,nomTrans,null);
				} else if (s.startsWith("~h")) {
					String nomHMM = s.substring(s.indexOf('"') + 1, s.lastIndexOf('"'));
					if (!nomHMM.toUpperCase().equals(nomHMM)) {
						System.err.println("ERROR: the HTK models contain lower-case phone names !");
						System.err.println("Please use the tool edu.cmu.sphinx.linguist.acoustic.tiedstate.HTK.NamesConversion");
						System.err.println("to convert the model files and the lexicon before...");
						System.exit(1);
					}
					hmms.add(loadHMM(f, nomHMM, gmms));
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String [][] tiedHMMs;
	public void loadTiedList(String nomFich) {
		try {
		    BufferedReader f = new BufferedReader(new FileReader(nomFich));
		    String s;
		    String [] ss;
		    int ntiedstates = 0;
			for (;;) {
				s = f.readLine();
				if (s == null)
					break;
				ss=s.split(" ");
				if (ss.length >= 2) {
					// on a un tiedstate !
					ntiedstates++;
				}
			}
			tiedHMMs = new String[ntiedstates][2];
			f.close();
		    f = new BufferedReader(new FileReader(nomFich));
			for (int i=0;;) {
				s = f.readLine();
				if (s == null)
					break;
				ss=s.split(" ");
				if (ss.length >= 2) {
					// on a un tiedstate !
					tiedHMMs[i][0] = ss[0];
					tiedHMMs[i++][1] = ss[1];
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * WARNING
	 * To be compliant with sphinx3 models, we remove the first non-emitting state !
	 * 
	 * @param f
	 * @param n
	 * @param autresEtats
	 * @throws IOException
	 */
	private SingleHMM loadHMM(BufferedReader f, String n, List<GMMDiag> autresEtats) throws IOException {
		GMMDiag e=null;
		int curstate;
		String nom=n;
		String s = "";
		
		while (!s.startsWith("<NUMSTATES>")) {
		    s=f.readLine();
		}
		int nstates = Integer.parseInt(s.substring(s.indexOf(' ')+1));
		// compliancy with sphinx3
		nstates--;
		SingleHMM theHMM = new SingleHMM(nstates);
		theHMM.setNom(n);
		theHMM.hmmset = this;
		while (!s.startsWith("<STATE>")) s=f.readLine();
		while (s.startsWith("<STATE>")) {
		    curstate = Integer.parseInt(s.substring(s.indexOf(' ')+1));
			// compliancy with sphinx3
		    curstate--;
		    s=f.readLine();
		    int gmmidx=-1;
		    if (s.startsWith("~s")) {
		    	String nomEtat = s.substring(s.indexOf('"')+1,s.lastIndexOf('"'));
		    	int i;
		    	for (i=0;i<autresEtats.size();i++) {
		    		e = autresEtats.get(i);
		    		if (e.nom.equals(nomEtat)) break;
		    	}
		    	gmmidx=i;
		    	if (i==autresEtats.size()) {
		    		System.err.println("erreur new hmm : etat "+nom+" non trouve");
		    		System.exit(1);
		    	}
		    } else {
		    	loadEtat(f,"",s);
		    	gmmidx = gmms.size()-1;
		    	e= gmms.get(gmms.size() - 1);
		    }
	    	HMMState st = new HMMState(e,new Lab(nom,curstate));
	    	st.gmmidx = gmmidx;
	    	etats.add(st);
	    	theHMM.setState(curstate-1,st); // -1 car dans HTK, les HMMs comptent a partir de 1
		    s=f.readLine();
		    // on elimine le gconst car il est recalcule ensuite !
		    if (s.startsWith("<GCONST>")) s=f.readLine();
		}
		if (s.startsWith("~t")) {
			// simple appel de macro
			String nomTrans = s.substring(s.indexOf('"') + 1, s.lastIndexOf('"'));
		    int tridx = getTrans(nomTrans);
			theHMM.setTrans(tridx);
		} else {
			// les trans sont explicites
			if (!s.startsWith("<TRANSP>")) {
			    System.err.println("erreur new hmm : pas de trans ? "+s);
			    System.exit(1);
			}
		    loadTrans(f,null,s);
			theHMM.setTrans(trans);
		}
		s=f.readLine();
		if (!s.startsWith("<ENDHMM>")) {
		    System.err.println("erreur new hmm : pas de fin ? "+s);
		    System.exit(1);
		}
		return theHMM;
	}
	
	private int loadTrans(BufferedReader f, String nomEtat, String prem) throws IOException {
		String s;
		int nstates=0;
		if (prem != null)
			s = prem;
		else
			s = f.readLine().trim();
		if (s.startsWith("<TRANSP>")) {
			nstates = Integer.parseInt(s.substring(s.indexOf(' ') + 1));
			// compliancy with sphinx3
			nstates--;
		} else {
			System.err.println("ERROR no TRANSP !");
			System.exit(1);
		}
		String [] ss;
	    trans = new float[nstates][nstates];
		// compliancy with sphinx3
	    f.readLine();
		for (int i=0;i<nstates;i++) {
		    s=f.readLine().trim();
		    ss=s.split(" ");
		    for (int j=0;j<nstates;j++) {
				// compliancy with sphinx3
		    	trans[i][j]=Float.parseFloat(ss[j+1]);
		    }
		}
		if (nomEtat!=null) {
			int tridx = transitions.size();
			transNames.put(nomEtat,tridx);
			transitions.add(trans);
			return tridx;
		} else {
			return -1;
			// l'appelant pourra recuperer la trans dans trans[][]
		}
	}
	
	private int getTrans(String trnom) {
		int tridx = transNames.get(trnom);
		return tridx;
	}
	
	private void loadEtat(BufferedReader f, String nomEtat, String prem)
			throws IOException {
		ngauss = 1;
		String s;
		if (prem != null)
			s = prem;
		else
			s = f.readLine().trim();
		if (s.startsWith("<NUMMIXES>")) {
			ngauss = Integer.parseInt(s.substring(s.indexOf(' ') + 1));
			s = f.readLine().trim();            
        }
        g = null;
        if (!s.startsWith("<MIXTURE>")) {            
            // cas particulier a 1 mixture            
            if (ngauss!=1) {                
                System.err.println("erreur gmm loadHTK: n mixes "+ngauss+" mais pas de mixture ! "+s);                
                System.exit(1);                
            }            
            loadHTKGauss(f,0,s);
            g.setWeight(0,1f);
        } else {            
            String [] ss;            
            for (int i=0;i<ngauss;i++) {                
                if (i>0) s=f.readLine().trim();
                // on ne charge pas le gconst !
                if (s.startsWith("<GCONST>")) s=f.readLine().trim();                
                ss = s.split(" ");
                if (Integer.parseInt(ss[1])!=i+1) {
                    System.err.println("erreur gmm loadHTK: mixture conflict "+i+ ' ' +s);
                    System.exit(1);
                }
                loadHTKGauss(f,i,null);                
                g.setWeight(i,Float.parseFloat(ss[2]));
            }            
        }        
        g.precomputeDistance();
        g.setNom(nomEtat);
        gmms.add(g);
	}

    /** lit jusqu'a la derniere ligne d'une gauss, mais laisse la ligne suivante en place !     *
     * Il peut donc rester un <GCONST> en place */    
    private void loadHTKGauss(BufferedReader f, int n, String prem) throws IOException {        
        String s;        
        String [] ss;        
        if (prem!=null) {            
            // premiere ligne a prendre en compte            
            s = prem;            
        } else            
            s=f.readLine().trim();        
        if (s.startsWith("<GCONST>")) s=f.readLine().trim();   
        if (s.startsWith("<RCLASS>")) s=f.readLine().trim();
        if (!s.startsWith("<MEAN>")) {            
            System.err.println("erreur gmm loadHTK: pas de <MEAN> ! "+s);            
            System.exit(1);            
        }        
        int ncoefs = Integer.parseInt(s.substring(s.indexOf(' ')+1));
        if (g==null) g= new GMMDiag(ngauss,ncoefs);
        s=f.readLine().trim();        
        ss=s.split(" ");        
        if (ss.length != ncoefs) {            
            System.err.println("erreur gmm loadHTK: pas le bon nb de coefs "+ncoefs+ ' ' +s+ ' ' +ss[0]+ ' ' +ss[39]);
            System.exit(1);            
        }   
        for (int i=0;i<ncoefs;i++) {            
        	g.setMean(n,i,Float.parseFloat(ss[i]));
        }        
        s=f.readLine().trim();        
        if (!s.startsWith("<VARIANCE>")) {            
            System.err.println("erreur gmm loadHTK: pas de <VARIANCE> ! "+s);            
            System.exit(1);            
        }        
        s=f.readLine().trim();        
        ss=s.split(" ");        
        if (ss.length != ncoefs) {            
            System.err.println("erreur gmm loadHTK: pas le bon nb de coefs "+ncoefs+ ' ' +s);
            System.exit(1);            
        }        
        for (int i=0;i<ncoefs;i++) {            
        	g.setVar(n,i,Float.parseFloat(ss[i]));
        }        
    }    
	
    public GMMDiag findState(Lab l) {
    	HMMState s=null;
    	int i;
    	for (i=0;i<etats.size();i++) {
    		s = etats.get(i);
    		if (s.getLab().isEqual(l)) break;
    	}
    	if (i<etats.size()) {
    		return s.gmm;
    	} else {
    		if (tiedHMMs!=null) {
    			// peut-etre que l'etat apparait dans la liste des tiedstates ?!
    			for (i=0;i<tiedHMMs.length;i++) {
    				if (tiedHMMs[i][0].equals(l.getName())) {
    					break;
    				}
    			}
    			if (i<tiedHMMs.length) {
    				return findState(new Lab(tiedHMMs[i][1],l.getState()));
    			}
    		}
    		System.err.println("WARNING hmmset findstate not found "+l);
    		return null;
    	}
    }
}

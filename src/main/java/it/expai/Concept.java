package it.expai;

public class Concept extends MembershipAssertion{
	private String term;

	public Concept(String ns, String ln, String t) {
		super(ns, ln);
		this.term = t;
	}
		
	public void setTerm(String t) {
		this.term = t;
	}

	public String getConceptTerm() {
		return this.term;
	}

	public String getConceptName() {
		return "<"+this.getNamespace()+this.getLocalName()+">";	
	}
	
	public String toString() {
		return super.toString()+"("+ this.term +")";
	}
	public String toStringShort() {
		return super.toStringShort()+"("+ this.term +")";
	}
	public String toStringShortPrefix(String p) {
		return super.toStringShortPrefix(p)+"("+ this.term +")";
	}
}

package it.expai;

public class Role extends MembershipAssertion{
	private String domain_term;
	private String range_term;
	
	public Role(String ns, String ln, String t1, String t2) {
		super(ns, ln);
		this.domain_term = 	t1;
		this.range_term  = 	t2;
	}
		
	public void setTerms(String t1, String t2) {
		this.domain_term = t1;
		this.range_term = t2;
	}

	public String getDomainTerm() {
		return this.domain_term;
	}
	public String getRangeTerm() {
		return this.range_term;
	}
	
	public String toString() {
		return super.toString()+"("+this.domain_term+", "+this.range_term+")";
	}
	public String toStringShort() {
		return super.toStringShort()+"("+this.domain_term+", "+this.range_term+")";
	}
	public String toStringShortPrefix(String p) {
		return super.toStringShortPrefix(p)+"("+this.domain_term+", "+this.range_term+")";
	}
	
}

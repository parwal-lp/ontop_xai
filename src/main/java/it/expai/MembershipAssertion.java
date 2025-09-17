package it.expai;

public class MembershipAssertion {
	private String namespace;
	private String localName;
	
	public MembershipAssertion(String namespace, String localName) {
		this.namespace = namespace;
		this.localName = localName;
	}
	
	public String getNamespace() {
		return this.namespace;
	}

	public String getLocalName() {
		return this.localName;
	}

	public String getName() {
		return this.namespace + this.localName;
	}
	
	public String toString() {
		return this.getName();
	}
	public String toStringShort() {
		return this.localName;
	}
	public String toStringShortPrefix(String p) {
		return p+this.localName;
	}
}

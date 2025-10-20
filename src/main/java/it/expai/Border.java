package it.expai;

import java.util.Set;

public class Border {
    private int radius;
    private Set<MembershipAssertion> assertions;
    private Set<String> terms;
    
    public Border(int radius, Set<MembershipAssertion> assertions, Set<String> terms) {
        this.radius = radius;
        this.assertions = assertions;
        this.terms = terms;
    }

    public int getRadius() {
        return radius;
    }

    public Set<MembershipAssertion> getAssertions() {
        return assertions;
    }
    
    public Set<String> getTerms() {
        return terms;
    }
}
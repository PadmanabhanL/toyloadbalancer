package com.sample.lb.bo;

import java.util.List;

public class LoadBalancer {

    List<String> targetHosts;

    BalancingStrategy balancingStrategy;

    public LoadBalancer(List<String> targetHosts, BalancingStrategy balancingStrategy) {
        this.targetHosts = targetHosts;
        this.balancingStrategy = balancingStrategy;
    }

    public BalancingStrategy getBalancingStrategy() {
        return balancingStrategy;
    }

    public void setBalancingStrategy(BalancingStrategy balancingStrategy) {
        this.balancingStrategy = balancingStrategy;
    }

    public List<String> getTargetHosts() {
        return targetHosts;
    }

    public void setTargetHosts(List<String> targetHosts) {
        this.targetHosts = targetHosts;
    }
}

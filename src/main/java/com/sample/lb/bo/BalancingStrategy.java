package com.sample.lb.bo;

public enum BalancingStrategy {

    //Static
    ROUND_ROBIN,

    STICKY_ROUND_ROBIN,

    WEIGHTED_ROUND_ROBIN,

    HASH_BASED,

    //Dynamic
    LEAST_CONNECTION,

    LEAST_TIME
}

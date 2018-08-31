package sviolet.slate.springboot.modelx.loadbalance.auto;

import sviolet.slate.common.modelx.loadbalance.classic.SimpleOkHttpClient;

import java.util.Set;

public interface HttpClients {

    SimpleOkHttpClient get(String tag);

    int size();

    Set<String> tags();

}

package sviolet.slate.common.utilx.txtimer.def;

class Record {

    //组名
    private String groupName;
    //交易名
    private String transactionName;
    //开始时间
    private long startTime;

    Record(String groupName, String transactionName) {
        this.groupName = groupName;
        this.transactionName = transactionName;
        this.startTime = System.currentTimeMillis();
    }

    String getGroupName() {
        return groupName;
    }

    String getTransactionName() {
        return transactionName;
    }

    long getStartTime() {
        return startTime;
    }
}

package sviolet.slate.common.utilx.txtimer.def;

class Record {

    private String groupName;
    private String transactionName;
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

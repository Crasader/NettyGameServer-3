package com.game.threadpool.common.enums;

/**
 * 
 * @author JiangBangMing
 *
 * 2018年6月27日 上午11:57:21
 */
public enum BlockingQueueType {
    LINKED_BLOCKING_QUEUE("LinkedBlockingQueue"),
    ARRAY_BLOCKING_QUEUE("ArrayBlockingQueue"),
    SYNCHRONOUS_QUEUE("SynchronousQueue");

    private String value;

    private BlockingQueueType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static BlockingQueueType fromString(String value) {
        for (BlockingQueueType type : BlockingQueueType.values()) {
            if (type.getValue().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException("Mismatched type with value=" + value);
    }

    public String toString() {
        return value;
    }
}

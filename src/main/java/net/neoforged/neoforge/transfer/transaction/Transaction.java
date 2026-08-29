package net.neoforged.neoforge.transfer.transaction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * 兼容层：模拟 NeoForge 26.x 的根事务（快照式回滚）。
 * <p>
 * 实现为简单的回调列表：修改方通过 {@link #addRollback} 注册撤销动作、
 * 通过 {@link #addCommit} 注册提交回调。{@link #commit()} 触发提交回调并丢弃撤销列表；
 * {@link #rollback()} 逆序执行撤销回调。未显式提交时 {@link #close()} 视为回滚。
 * </p>
 */
public class Transaction implements TransactionContext, AutoCloseable {

    private final Deque<Runnable> rollbacks = new ArrayDeque<>();
    private final Deque<Runnable> commits = new ArrayDeque<>();
    private boolean active = true;

    /** 打开新的根事务。 */
    public static Transaction openRoot() {
        return new Transaction();
    }

    /** 注册撤销动作（回滚时逆序执行）。 */
    public void addRollback(Runnable rollback) {
        Objects.requireNonNull(rollback, "rollback");
        if (active) {
            rollbacks.push(rollback);
        }
    }

    /** 注册提交回调（提交时按注册顺序执行）。 */
    public void addCommit(Runnable commit) {
        Objects.requireNonNull(commit, "commit");
        if (active) {
            commits.add(commit);
        }
    }

    /** 提交事务：执行提交回调并清空全部回调。 */
    public void commit() {
        if (!active) {
            return;
        }
        active = false;
        Deque<Runnable> commits = this.commits;
        rollbacks.clear();
        for (Runnable commit : commits) {
            commit.run();
        }
        this.commits.clear();
    }

    /** 回滚事务：逆序执行撤销回调并清空全部回调。 */
    public void rollback() {
        if (!active) {
            return;
        }
        active = false;
        Deque<Runnable> rollbacks = this.rollbacks;
        commits.clear();
        for (Runnable rollback : rollbacks) {
            rollback.run();
        }
        this.rollbacks.clear();
    }

    /** 未显式提交/回滚时，关闭视为回滚。 */
    @Override
    public void close() {
        if (active) {
            rollback();
        }
    }

    /** 当前事务是否仍处于活动状态。 */
    public boolean isActive() {
        return active;
    }
}

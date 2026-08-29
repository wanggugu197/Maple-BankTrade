package net.neoforged.neoforge.transfer.transaction;

/**
 * 兼容层：模拟 NeoForge 26.x 的快照日志（SnapshotJournal）。
 * <p>
 * 修改前调用 {@link #updateSnapshots(TransactionContext)} 记录当前状态：
 * 事务提交时触发 {@link #onRootCommit(Object)}，回滚时恢复 {@link #createSnapshot()} 的状态。
 * </p>
 *
 * @param <T> 快照类型
 */
public abstract class SnapshotJournal<T> {

    /** 在事务中记录当前快照；非本兼容层事务时不做任何事。 */
    public void updateSnapshots(TransactionContext transaction) {
        if (transaction instanceof Transaction tx) {
            T snapshot = createSnapshot();
            tx.addRollback(() -> revertToSnapshot(snapshot));
            tx.addCommit(() -> onRootCommit(snapshot));
        }
    }

    /** 创建当前状态快照。 */
    protected abstract T createSnapshot();

    /** 回滚到快照状态。 */
    protected abstract void revertToSnapshot(T snapshot);

    /** 根事务提交时回调（默认无操作）。 */
    protected void onRootCommit(T originalState) {}
}

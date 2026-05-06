package com.aigc.common.id;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间戳 ID 生成器
 *
 * <p>格式：{@code yyyyMMddHHmmss(14) + workerId(2) + sequence(3) = 19 位}
 * <pre>
 *   2025050615302101001
 *   ├─ 20250506153021  → 2025-05-06 15:30:21
 *   ├─ 01              → workerId = 1
 *   └─ 001             → 当前秒内第 1 个
 * </pre>
 *
 * <ul>
 *   <li>容量：每节点每秒最多 1000 个，31 个节点共 31000/s</li>
 *   <li>有序：同节点同秒内单调递增</li>
 *   <li>Long 范围：最大值约 2099123123595931999，远小于 Long.MAX_VALUE（9.2×10¹⁸）</li>
 * </ul>
 */
public class TimestampIdGenerator implements IdentifierGenerator {

    private static final DateTimeFormatter FMT     = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final long              SEQ_MAX = 1_000L;  // 每秒最多 1000 个序列

    private final long workerId;

    private long lastSecond = -1L;
    private long sequence   = 0L;

    /**
     * @param workerId 节点编号，范围 0-31，多实例部署时每个实例取不同值
     */
    public TimestampIdGenerator(long workerId) {
        if (workerId < 0 || workerId > 31) {
            throw new IllegalArgumentException("workerId 必须在 0-31 之间，实际值：" + workerId);
        }
        this.workerId = workerId;
    }

    @Override
    public synchronized Long nextId(Object entity) {
        long nowSecond = System.currentTimeMillis() / 1000;

        if (nowSecond == lastSecond) {
            sequence = (sequence + 1) % SEQ_MAX;
            if (sequence == 0) {
                // 当前秒序列已耗尽，自旋等待下一秒（正常情况极少触发）
                while ((nowSecond = System.currentTimeMillis() / 1000) == lastSecond) {
                    Thread.onSpinWait();
                }
            }
        } else {
            sequence = 0;
        }
        lastSecond = nowSecond;

        // 用 LocalDateTime 格式化，保证时区对齐
        String ts = LocalDateTime.now().format(FMT);
        // yyyyMMddHHmmss × 100_000 + workerId × 1_000 + sequence
        return Long.parseLong(ts) * 100_000L + workerId * 1_000L + sequence;
    }
}

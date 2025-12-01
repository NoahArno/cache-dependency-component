package top.noaharno.cachedependency.constant;

import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;

/**
 * 缓存新鲜度枚举类
 *
 * @author NoahArno
 * @version 1.0.0
 * @since 2025/11/27 18:17
 */
@Getter
public enum CacheLevelEnum {

    /**
     * 秒级新鲜度
     */
    SECONDS(1),

    /**
     * 分钟级新鲜度
     */
    MINUTES(2),

    /**
     * 小时级新鲜度
     */
    HOURS(3),

    /**
     * 天级新鲜度
     */
    DAYS(4);

    /**
     * 新鲜度级别，值越小，新鲜度越高
     */
    private final int level;

    CacheLevelEnum(int level) {
        this.level = level;
    }

    /**
     * 按照新鲜度级别排序
     * @return 新鲜度级别排序后的枚举数组
     */
    public static CacheLevelEnum[] getSortedValues() {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(CacheLevelEnum::getLevel))
                .toArray(CacheLevelEnum[]::new);
    }

}

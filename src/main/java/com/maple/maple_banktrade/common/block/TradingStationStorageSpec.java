package com.maple.maple_banktrade.common.block;

/**
 * 贸易站库存规格：由子类在构造时传入，基类据此创建物品/流体/能量 handler。
 * <p>
 * 能量单次充入/抽出上限均等于 {@code energyCapacity}。
 * </p>
 *
 * @param itemInputSlots          物品输入槽位数（≥0）
 * @param itemOutputSlots         物品输出槽位数（≥0）
 * @param itemStackSizeMultiplier 物品默认堆叠倍率（≤0 视为 1）
 * @param fluidInputTanks         流体输入罐数（≥0）
 * @param fluidOutputTanks        流体输出罐数（≥0）
 * @param fluidCapacityMb         单罐流体容量 mB（≥0）
 * @param energyCapacity          能量总容量，同时作为单次 transfer 上限（≥0）
 */
public record TradingStationStorageSpec(
                                        int itemInputSlots,
                                        int itemOutputSlots,
                                        float itemStackSizeMultiplier,
                                        int fluidInputTanks,
                                        int fluidOutputTanks,
                                        int fluidCapacityMb,
                                        int energyCapacity) {

    public TradingStationStorageSpec {
        itemInputSlots = Math.max(0, itemInputSlots);
        itemOutputSlots = Math.max(0, itemOutputSlots);
        itemStackSizeMultiplier = itemStackSizeMultiplier <= 0f ? 1f : itemStackSizeMultiplier;
        fluidInputTanks = Math.max(0, fluidInputTanks);
        fluidOutputTanks = Math.max(0, fluidOutputTanks);
        fluidCapacityMb = Math.max(0, fluidCapacityMb);
        energyCapacity = Math.max(0, energyCapacity);
    }

    /** 全功能交易站默认规格。 */
    public static TradingStationStorageSpec fullStation() {
        return builder()
                .itemSlots(24, 24)
                .fluidTanks(6, 6, 64_000)
                .energy(Integer.MAX_VALUE)
                .build();
    }

    /** 物品卡贸易站默认规格（较小库存）。 */
    public static TradingStationStorageSpec itemCardStation() {
        return builder()
                .itemSlots(18, 18)
                .fluidTanks(2, 2, 16_000)
                .energy(100_000)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private int itemInputSlots = 9;
        private int itemOutputSlots = 9;
        private float itemStackSizeMultiplier = 1f;
        private int fluidInputTanks = 0;
        private int fluidOutputTanks = 0;
        private int fluidCapacityMb = 16_000;
        private int energyCapacity = 0;

        public Builder itemSlots(int input, int output) {
            this.itemInputSlots = input;
            this.itemOutputSlots = output;
            return this;
        }

        public Builder itemStackSizeMultiplier(float multiplier) {
            this.itemStackSizeMultiplier = multiplier;
            return this;
        }

        public Builder fluidTanks(int input, int output, int capacityMb) {
            this.fluidInputTanks = input;
            this.fluidOutputTanks = output;
            this.fluidCapacityMb = capacityMb;
            return this;
        }

        public Builder energy(int capacity) {
            this.energyCapacity = capacity;
            return this;
        }

        public TradingStationStorageSpec build() {
            return new TradingStationStorageSpec(
                    itemInputSlots, itemOutputSlots, itemStackSizeMultiplier,
                    fluidInputTanks, fluidOutputTanks, fluidCapacityMb,
                    energyCapacity);
        }
    }
}

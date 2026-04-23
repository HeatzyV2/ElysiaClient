package net.elysiastudios.client.module.impl.smartfight;

import java.util.Collection;
import java.util.Deque;

final class AnalysisMath {
    private AnalysisMath() {
    }

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static float lerp(float from, float to, float delta) {
        return from + (to - from) * clamp(delta, 0.0F, 1.0F);
    }

    static double lerp(double from, double to, double delta) {
        return from + (to - from) * clamp(delta, 0.0D, 1.0D);
    }

    static double averageDouble(Collection<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0D;
        }

        double sum = 0.0D;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    static double averageLong(Collection<Long> values) {
        if (values == null || values.isEmpty()) {
            return 0.0D;
        }

        double sum = 0.0D;
        for (long value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    static float averageFloat(Collection<Float> values) {
        if (values == null || values.isEmpty()) {
            return 0.0F;
        }

        float sum = 0.0F;
        for (float value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    static double stdDevDouble(Collection<Double> values, double average) {
        if (values == null || values.size() < 2) {
            return 0.0D;
        }

        double variance = 0.0D;
        for (double value : values) {
            double diff = value - average;
            variance += diff * diff;
        }
        return Math.sqrt(variance / values.size());
    }

    static double stdDevLong(Collection<Long> values, double average) {
        if (values == null || values.size() < 2) {
            return 0.0D;
        }

        double variance = 0.0D;
        for (long value : values) {
            double diff = value - average;
            variance += diff * diff;
        }
        return Math.sqrt(variance / values.size());
    }

    static float booleanRatio(Deque<Boolean> values) {
        if (values == null || values.isEmpty()) {
            return 0.0F;
        }

        int positives = 0;
        for (boolean value : values) {
            if (value) {
                positives++;
            }
        }
        return (float) positives / (float) values.size();
    }

    static float ratioAtLeast(Collection<Float> values, float threshold) {
        if (values == null || values.isEmpty()) {
            return 0.0F;
        }

        int matching = 0;
        for (float value : values) {
            if (value >= threshold) {
                matching++;
            }
        }
        return (float) matching / (float) values.size();
    }

    static float ratioBelow(Collection<Long> values, double threshold) {
        if (values == null || values.isEmpty()) {
            return 0.0F;
        }

        int matching = 0;
        for (long value : values) {
            if (value < threshold) {
                matching++;
            }
        }
        return (float) matching / (float) values.size();
    }
}

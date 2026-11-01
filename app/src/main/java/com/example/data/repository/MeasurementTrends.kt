package com.example.data.repository

import com.example.data.local.entity.BodyMeasurementEntity

data class MetricTrend(val current: Float?, val change: Float?)
data class MeasurementTrends(val weight: MetricTrend, val bodyFat: MetricTrend, val muscle: MetricTrend, val waist: MetricTrend) {
    companion object {
        fun from(measurements: List<BodyMeasurementEntity>): MeasurementTrends {
            val ordered = measurements.sortedWith(compareByDescending<BodyMeasurementEntity> { it.date }.thenByDescending { it.id })
                .distinctBy { it.date }.reversed()
            fun trend(value: (BodyMeasurementEntity) -> Float?): MetricTrend {
                val values = ordered.mapNotNull { value(it)?.takeIf { number -> number.isFinite() && number > 0f } }
                return MetricTrend(values.lastOrNull(), if (values.size >= 2) values.last() - values.first() else null)
            }
            return MeasurementTrends(trend { it.weightKg }, trend { it.bodyFatPercent }, trend { it.skeletalMuscleKg }, trend { it.waistCm })
        }
    }
}
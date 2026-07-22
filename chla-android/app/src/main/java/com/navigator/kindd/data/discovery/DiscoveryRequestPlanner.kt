package com.navigator.kindd.data.discovery

import com.navigator.kindd.data.models.Provider
import java.util.Locale
import javax.inject.Inject

sealed interface PlannedDiscoveryRequest {
    data class ProfileZip(
        val remote: RegionalCenterProviderRequest,
        val query: String?,
        val therapies: Set<TherapyType>
    ) : PlannedDiscoveryRequest

    data class Comprehensive(
        val remote: ComprehensiveProviderRequest
    ) : PlannedDiscoveryRequest

    data object Catalog : PlannedDiscoveryRequest
}

class DiscoveryRequestPlanner @Inject constructor() {

    fun plan(criteria: DiscoveryCriteria): PlannedDiscoveryRequest {
        val query = normalizeWhitespace(criteria.query)
        val diagnosis = normalizeWhitespace(criteria.diagnosis)
        val insurance = normalizeWhitespace(criteria.insurance)
        val therapies = criteria.therapyTypes
            .sortedBy(TherapyType::apiValue)
            .toCollection(linkedSetOf())

        return when (val origin = criteria.origin) {
            is DiscoveryOrigin.ProfileZip -> PlannedDiscoveryRequest.ProfileZip(
                remote = RegionalCenterProviderRequest(
                    zipCode = normalizeWhitespace(origin.zipCode).orEmpty(),
                    ageGroup = criteria.ageGroup?.apiValue,
                    diagnosis = diagnosis,
                    insurance = insurance
                ),
                query = query,
                therapies = therapies
            )

            is DiscoveryOrigin.DeviceLocation -> PlannedDiscoveryRequest.Comprehensive(
                remote = comprehensiveRequest(
                    criteria = criteria,
                    query = query,
                    diagnosis = diagnosis,
                    insurance = insurance,
                    therapies = therapies,
                    latitude = origin.latitude,
                    longitude = origin.longitude
                )
            )

            DiscoveryOrigin.LosAngelesCatalog -> {
                val hasActiveFilter = query != null ||
                    therapies.isNotEmpty() ||
                    criteria.ageGroup != null ||
                    diagnosis != null ||
                    insurance != null

                if (hasActiveFilter) {
                    PlannedDiscoveryRequest.Comprehensive(
                        remote = comprehensiveRequest(
                            criteria = criteria,
                            query = query,
                            diagnosis = diagnosis,
                            insurance = insurance,
                            therapies = therapies
                        )
                    )
                } else {
                    PlannedDiscoveryRequest.Catalog
                }
            }
        }
    }

    fun applyLocalFilters(
        providers: List<Provider>,
        request: PlannedDiscoveryRequest.ProfileZip
    ): List<Provider> {
        val selectedTherapies = request.therapies
            .map { normalizeForComparison(it.apiValue) }
        val query = request.query?.let(::normalizeForComparison)

        return providers.filter { provider ->
            val providerTherapies = provider.therapyTypes.orEmpty()
                .map(::normalizeForComparison)
                .toSet()
            val hasEveryTherapy = selectedTherapies.all(providerTherapies::contains)
            val matchesQuery = query == null || searchableValues(provider).any { value ->
                normalizeForComparison(value).contains(query)
            }

            hasEveryTherapy && matchesQuery
        }
    }

    fun requestKey(request: PlannedDiscoveryRequest): String = when (request) {
        PlannedDiscoveryRequest.Catalog -> "catalog"
        is PlannedDiscoveryRequest.ProfileZip -> keyOf(
            "profileZip",
            request.remote.zipCode,
            request.remote.ageGroup,
            request.remote.diagnosis,
            request.remote.insurance,
            request.query,
            request.therapies.map(TherapyType::apiValue).sorted().joinToString("\u001F")
        )

        is PlannedDiscoveryRequest.Comprehensive -> keyOf(
            "comprehensive",
            request.remote.query,
            request.remote.latitude?.toString(),
            request.remote.longitude?.toString(),
            request.remote.radiusMiles.toString(),
            request.remote.therapyTypes.sorted().joinToString("\u001F"),
            request.remote.ageGroup,
            request.remote.diagnosis,
            request.remote.insurance
        )
    }

    private fun comprehensiveRequest(
        criteria: DiscoveryCriteria,
        query: String?,
        diagnosis: String?,
        insurance: String?,
        therapies: Set<TherapyType>,
        latitude: Double? = null,
        longitude: Double? = null
    ): ComprehensiveProviderRequest = ComprehensiveProviderRequest(
        query = query,
        latitude = latitude,
        longitude = longitude,
        radiusMiles = criteria.radiusMiles,
        therapyTypes = therapies.map(TherapyType::apiValue),
        ageGroup = criteria.ageGroup?.apiValue,
        diagnosis = diagnosis,
        insurance = insurance
    )

    private fun searchableValues(provider: Provider): List<String> = buildList {
        add(provider.name)
        add(provider.fullAddress)
        provider.city?.let(::add)
        provider.description?.let(::add)
        addAll(provider.therapyTypes.orEmpty())
        addAll(provider.insuranceAccepted)
    }

    private fun keyOf(vararg fields: String?): String = fields.joinToString("|") { field ->
        val value = field.orEmpty()
        "${value.length}:$value"
    }

    private fun normalizeForComparison(value: String): String =
        normalizeWhitespace(value).orEmpty().lowercase(Locale.ROOT)

    private fun normalizeWhitespace(value: String?): String? = value
        ?.trim()
        ?.replace(WHITESPACE, " ")
        ?.takeIf(String::isNotEmpty)

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}

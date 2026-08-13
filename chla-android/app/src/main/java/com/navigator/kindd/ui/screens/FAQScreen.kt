package com.navigator.kindd.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.navigator.kindd.R
import com.navigator.kindd.ui.theme.KiNDDBlue

data class FAQItem(
    val id: String,
    val question: String,
    val answer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FAQScreen(
    onBack: () -> Unit
) {
    var expandedId by remember { mutableStateOf<String?>(null) }

    val faqs = listOf(
        FAQItem(
            "aba-1",
            "What is ABA therapy?",
            "Applied Behavior Analysis (ABA) is a scientific, evidence-based therapy that helps improve specific behaviors, such as social skills, communication, reading, and academics. It is particularly effective for individuals with autism spectrum disorder."
        ),
        FAQItem(
            "aba-2",
            "Who can benefit from ABA therapy?",
            "ABA therapy can benefit individuals with autism spectrum disorder, developmental disabilities, learning disabilities, behavioral disorders, and communication disorders. While most commonly used for children with autism, it can help anyone needing structured behavioral intervention."
        ),
        FAQItem(
            "aba-3",
            "How long does ABA therapy take?",
            "Duration varies based on individual needs. Intensive programs run 20-40 hours per week, focused programs 10-20 hours, and consultation 1-5 hours. Most children participate for 1-3 years, though this varies based on progress and goals."
        ),
        FAQItem(
            "rc-1",
            "What is a Regional Center?",
            "Regional Centers are nonprofit agencies funded by the State of California that provide services and support to individuals with developmental disabilities. They serve as the main point of contact for accessing state-funded services including ABA therapy."
        ),
        FAQItem(
            "rc-2",
            "How do I find my Regional Center?",
            "Your Regional Center is determined by your ZIP code. Use our Regional Centers page or enter your ZIP code in the app to find which one serves your area. Los Angeles County is served by 7 Regional Centers."
        ),
        FAQItem(
            "rc-3",
            "Are Regional Center services free?",
            "Yes! Services through Regional Centers are free for eligible individuals with developmental disabilities. Eligibility is based on having a qualifying developmental disability that began before age 18."
        ),
        FAQItem(
            "ins-1",
            "Does insurance cover ABA therapy?",
            "Many insurance plans cover ABA therapy, especially for autism. California law requires private insurance to cover autism treatment. Medi-Cal also covers ABA therapy, and Regional Centers may fund services not covered by insurance."
        ),
        FAQItem(
            "ins-2",
            "What if I don't have insurance?",
            "You have several options: apply for Medi-Cal (California's Medicaid program), contact your Regional Center for state-funded services, ask providers about sliding scale fees, or check for grant programs and scholarships."
        ),
        FAQItem(
            "map-1",
            "How do I use the KiNDD map?",
            "Enter your ZIP code or allow location access, browse resources in your area, use filters to narrow by therapy type or insurance, then click on a resource for details and contact information."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.faq_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KiNDDBlue,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.faq_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(faqs) { faq ->
                FAQCard(
                    faq = faq,
                    isExpanded = expandedId == faq.id,
                    onToggle = {
                        expandedId = if (expandedId == faq.id) null else faq.id
                    }
                )
            }
        }
    }
}

@Composable
private fun FAQCard(
    faq: FAQItem,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Text(
                    text = faq.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

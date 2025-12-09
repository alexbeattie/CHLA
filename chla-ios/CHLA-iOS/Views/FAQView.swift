//
//  FAQView.swift
//  NDD Resources
//
//  Frequently Asked Questions with proper formatting
//

import SwiftUI

struct FAQView: View {
    @EnvironmentObject var appState: AppState
    @State private var expandedID: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                heroSection

                VStack(alignment: .leading, spacing: 20) {
                    abaSection
                    rcSection
                    insuranceSection
                    appSection
                    ctaSection
                }
                .padding(20)
            }
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("FAQ")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Hero
    private var heroSection: some View {
        VStack(spacing: 8) {
            Text("Frequently Asked Questions")
                .font(.title3)
                .fontWeight(.bold)
                .foregroundColor(.white)

            Text("ABA therapy, Regional Centers, and finding resources")
                .font(.subheadline)
                .foregroundColor(.white.opacity(0.9))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
        .background(Color.accentBlue)
    }

    // MARK: - ABA Section
    private var abaSection: some View {
        FAQCategory(title: "ABA Therapy Basics") {
            FAQItem(
                id: "aba1",
                question: "What is ABA therapy?",
                answer: "Applied Behavior Analysis (ABA) is a scientific, evidence-based therapy that helps improve behaviors like social skills, communication, and learning. It's particularly effective for individuals with autism and developmental disabilities, using positive reinforcement to teach new skills.",
                expandedID: $expandedID
            )
            FAQItem(
                id: "aba2",
                question: "Who can benefit from ABA therapy?",
                answer: "ABA therapy benefits individuals with:\n\n• Autism Spectrum Disorder (ASD)\n• Developmental disabilities\n• Learning disabilities\n• Behavioral disorders (ADHD, ODD)\n• Communication disorders\n\nWhile most common for children with autism, anyone needing structured behavioral intervention can benefit.",
                expandedID: $expandedID
            )
            FAQItem(
                id: "aba3",
                question: "How long does ABA therapy take?",
                answer: "Duration varies by need:\n\n• Intensive: 20-40 hours/week\n• Focused: 10-20 hours/week\n• Consultation: 1-5 hours/week\n\nMost children participate for 1-3 years. Your resource creates an individualized plan.",
                expandedID: $expandedID
            )
            FAQItem(
                id: "aba4",
                question: "What happens in an ABA session?",
                answer: "A typical session includes:\n\n1. Structured activities for specific goals\n2. Positive reinforcement for behaviors\n3. Data collection to track progress\n4. Real-world skill practice\n5. Parent/caregiver training\n\nSessions can be at home, clinics, or schools.",
                expandedID: $expandedID
            )
        }
    }

    // MARK: - Regional Centers Section
    private var rcSection: some View {
        FAQCategory(title: "Regional Centers") {
            FAQItem(
                id: "rc1",
                question: "What is a Regional Center?",
                answer: "Regional Centers are California nonprofit agencies that provide services for individuals with developmental disabilities. LA County has 7:\n\n• San Gabriel/Pomona (SG/PRC)\n• Harbor (HRC)\n• North LA County (NLACRC)\n• Eastern LA (ELARC)\n• South Central LA (SCLARC)\n• Westside (WRC)\n• Frank D. Lanterman (FDLRC)\n\nYour center is determined by ZIP code.",
                expandedID: $expandedID
            )
            FAQItem(
                id: "rc2",
                question: "How do I find my Regional Center?",
                answer: "Your Regional Center is assigned by ZIP code. Use our map:\n\n1. Allow location access or enter ZIP\n2. The map shows your RC boundary\n3. See resources in your area\n\nYou cannot choose your RC - it's based on where you live.",
                expandedID: $expandedID
            )
            FAQItem(
                id: "rc3",
                question: "How do I get RC services?",
                answer: "Steps to access services:\n\n1. Contact your RC for intake appointment\n2. Complete intake assessment\n3. Wait for eligibility determination (~120 days)\n4. Develop Individualized Program Plan (IPP)\n5. Choose from approved resources\n6. Begin services\n\nYou must have a developmental disability that began before age 18.",
                expandedID: $expandedID
            )
            FAQItem(
                id: "rc4",
                question: "Do I need an RC for ABA therapy?",
                answer: "No, multiple funding options exist:\n\n• Regional Center - Free for eligible individuals\n• Private Insurance - CA law requires ABA coverage\n• Medi-Cal - Covers eligible children\n• School District - Through IEP\n• Private Pay - Self-funded\n\nMany families combine funding sources.",
                expandedID: $expandedID
            )
        }
    }

    // MARK: - Insurance Section
    private var insuranceSection: some View {
        FAQCategory(title: "Insurance & Funding") {
            FAQItem(
                id: "ins1",
                question: "Does insurance cover ABA in California?",
                answer: "Yes! California law requires most plans to cover ABA for autism:\n\n• Commercial Insurance - Required up to age 21\n• Medi-Cal - Covers eligible children\n• Regional Center - Eligible individuals\n• TRICARE - Military families\n\nCheck your plan for specific benefits and copays.",
                expandedID: $expandedID
            )
            FAQItem(
                id: "ins2",
                question: "What insurance do resources accept?",
                answer: "Common accepted insurance:\n\n• Aetna, Anthem, Blue Shield, Cigna\n• Health Net, Kaiser, UnitedHealthcare\n• Medi-Cal plans (LA Care, Molina, etc.)\n• TRICARE, Magellan\n• Regional Center funding\n\nUse our map filters to find resources accepting your insurance.",
                expandedID: $expandedID
            )
            FAQItem(
                id: "ins3",
                question: "How much does ABA therapy cost?",
                answer: "Costs depend on funding:\n\n• Regional Center - Free\n• Insurance - $0-75 copay per session\n• Medi-Cal - No cost\n• Private Pay - $50-150+ per hour\n\nIntensive programs can cost $30,000-70,000+ yearly without coverage.",
                expandedID: $expandedID
            )
            FAQItem(
                id: "ins4",
                question: "Do I need a diagnosis for ABA?",
                answer: "Yes, typically:\n\n• Insurance - Autism diagnosis required\n• Regional Center - Developmental disability diagnosis\n• School - IEP with documented need\n\nDiagnoses can come from psychologists, developmental pediatricians, psychiatrists, or neurologists.",
                expandedID: $expandedID
            )
        }
    }

    // MARK: - App Section
    private var appSection: some View {
        FAQCategory(title: "Using the App") {
            FAQItem(
                id: "app1",
                question: "How do I use NDD Resources?",
                answer: "Simple steps:\n\n1. Allow location access (or enter ZIP)\n2. Browse resources on the map\n3. Filter by insurance, therapy type, etc.\n4. Tap a resource for details\n5. Call, email, or get directions\n\nThe map shows your Regional Center area.",
                expandedID: $expandedID
            )
            FAQItem(
                id: "app2",
                question: "Are all LA County resources listed?",
                answer: "We have 370+ verified resources across LA County, covering all 7 Regional Center areas. We continuously add resources.\n\nNote: Listing does not constitute endorsement. Always verify credentials directly.",
                expandedID: $expandedID
            )
            FAQItem(
                id: "app3",
                question: "Is NDD Resources free?",
                answer: "Yes, completely free!\n\n✓ No registration\n✓ No fees or subscriptions\n✓ No credit card needed\n✓ Unlimited searches\n\nNDD Resources is a public service to help LA County families find developmental disability services.",
                expandedID: $expandedID
            )
        }
    }

    // MARK: - CTA
    private var ctaSection: some View {
        VStack(spacing: 12) {
            Text("Ready to Find a Resource?")
                .font(.headline)
                .foregroundColor(.white)

            Button {
                appState.navigateToMap()
            } label: {
                Label("Explore the Map", systemImage: "map.fill")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.accentBlue)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(Color.white)
                    .cornerRadius(8)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(20)
        .background(Color.accentBlue)
        .cornerRadius(12)
    }
}

// MARK: - FAQ Category

struct FAQCategory<Content: View>: View {
    let title: String
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.headline)
                .foregroundColor(.accentBlue)

            content()
        }
    }
}

// MARK: - FAQ Item

struct FAQItem: View {
    let id: String
    let question: String
    let answer: String
    @Binding var expandedID: String?

    private var isExpanded: Bool { expandedID == id }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Question - entire row is tappable
            Button {
                withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                    expandedID = isExpanded ? nil : id
                }
            } label: {
                HStack(spacing: 12) {
                    Text(question)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.primary)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    // Chevron with larger tap target and rotation
                    Image(systemName: "chevron.right")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(.accentBlue)
                        .rotationEffect(.degrees(isExpanded ? 90 : 0))
                        .frame(width: 32, height: 32)
                        .background(
                            Circle()
                                .fill(Color.accentBlue.opacity(0.1))
                        )
                }
                .padding(14)
                .contentShape(Rectangle())
            }
            .buttonStyle(FAQButtonStyle())

            // Answer with slide animation
            if isExpanded {
                VStack(alignment: .leading, spacing: 0) {
                    Divider()
                        .padding(.horizontal, 14)

                    Text(answer)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineSpacing(4)
                        .padding(14)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .background(Color(.systemBackground))
        .cornerRadius(10)
        .shadow(color: .black.opacity(0.03), radius: 2, y: 1)
    }
}

// Custom button style for better tap feedback
struct FAQButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(
                configuration.isPressed ? Color(.systemGray5) : Color.clear
            )
            .scaleEffect(configuration.isPressed ? 0.98 : 1.0)
            .animation(.easeInOut(duration: 0.1), value: configuration.isPressed)
    }
}

#Preview {
    NavigationStack {
        FAQView()
            .environmentObject(AppState())
    }
}

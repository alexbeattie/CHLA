<template>
  <div class="funding-info-modal" v-if="showModal">
    <div class="funding-info-content">
      <div class="funding-header">
        <h3>Funding Sources for Services</h3>
        <button @click="closeModal" class="btn-close" aria-label="Close"></button>
      </div>

      <div class="funding-body">
        <p class="lead">
          There are 4 primary sources of funding for services available to individuals with Autism Spectrum 
          Disorders (ASD) and other developmental conditions.
        </p>

        <div class="funding-sources">
          <div class="funding-source">
            <h4>
              <span class="funding-icon private-pay">
                <i class="bi bi-wallet2"></i>
              </span>
              1. Private Therapy
            </h4>
            <p>Parents may choose to pay for private therapies that are not covered by insurance 
              or offered by their Regional Center or school system.</p>
          </div>

          <div class="funding-source">
            <h4>
              <span class="funding-icon regional-center">
                <i class="bi bi-building"></i>
              </span>
              2. Regional Center Services
            </h4>
            <p>Infants or toddlers may be eligible for services through the Regional Center if they have one or more of the following:</p>
            <ul>
              <li>An established risk condition (a diagnosed medical condition that has a high probability of resulting in a developmental delay or disability).</li>
              <li>A one-third delay in one or more areas of development before 24 months, or after 24 months, a fifty percent delay in one area or a one-third delay in two or more developmental areas.</li>
            </ul>
            <p>Children ages 3 and older may be eligible for Regional Center services if they have a Developmental Disability. 
              More specifically, mental retardation, epilepsy, cerebral palsy, or autism, are all conditions that qualify a child to 
              receive services through the Regional Center.</p>
            <p>You must apply for your child to become a consumer of the Regional Center (RC), and the RC will complete an evaluation 
              to assess your child's development. Upon completion of the evaluation, a determination of eligibility will be made. 
              The RC can provide an array of services, ranging from respite to parent support groups, social skills groups, behavior intervention, 
              and numerous others. RC services can be secured for your child through their development into adulthood.</p>
          </div>

          <div class="funding-source">
            <h4>
              <span class="funding-icon school-based">
                <i class="bi bi-book"></i>
              </span>
              3. School Based Services
            </h4>
            <p>A child who has difficulty learning and functioning in the school setting is also eligible for support services. 
              The presence of a disability doesn't automatically guarantee services. The disability must have a negative impact on 
              the child's functioning at school.</p>
            <p>Eligibility is determined by having a multidisciplinary team of professionals evaluate a child using standardized tests, 
              observations, and evaluations of work samples. Parents and educators subsequently come together to review the evaluation 
              findings and develop the IEP as means of promoting a child's success in the school environment.</p>
            <p>The IEP describes the goals that are most appropriate for a child during the course of a school year and identifies 
              the supports necessary for the child to achieve those goals. Supports can range from academic modification and extended 
              time on activities, to having speech therapy services or a one to one behavioral aide.</p>
            <p>The IEP should be reviewed annually to update goals and ensure that the appropriate levels of support and services are in place. 
              As a parent, you can request an IEP at any time to discuss your concerns and request more, fewer, or different services.</p>
          </div>

          <div class="funding-source">
            <h4>
              <span class="funding-icon insurance">
                <i class="bi bi-shield-check"></i>
              </span>
              4. Services Covered by Health Insurance
            </h4>
            <p>Effective July 1, 2012, a law has taken effect, which mandates insurance companies to cover behavioral health treatment 
              for autism or pervasive developmental disorder. "Behavioral health treatment" includes Applied Behavior Analysis (ABA) 
              and other evidence-based therapies. Speech and occupational therapy services are also covered by this law. 
              Health insurers may require prior authorization, copayments, and/or other cost-sharing.</p>
            <p>The process of accessing services through Insurance can be a daunting undertaking. The following insurance companies 
              typically provide coverage for intensive ABA treatment services:</p>
            <ul class="insurance-list">
              <li>Anthem Blue Cross</li>
              <li>Blue Shield of California</li>
              <li>Cigna</li>
              <li>Aetna</li>
              <li>Magellan Health Services</li>
              <li>United Behavioral Health</li>
              <li>LifeSynch (a Humana Company)</li>
            </ul>
          </div>
        </div>

        <div class="funding-note mt-4">
          <p><strong>Note:</strong> Working with the insurance companies to access behavioral services is a rapidly changing process. 
            We recommend that you check with services regularly as processes involved in accessing therapy through insurance 
            may change.</p>
        </div>
      </div>

      <div class="funding-footer">
        <button @click="closeModal" class="btn btn-secondary">Close</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "FundingInfoPanel",
  
  props: {
    showModal: {
      type: Boolean,
      default: false
    }
  },
  
  methods: {
    closeModal() {
      this.$emit('close');
    }
  }
}
</script>

<style scoped>
.funding-info-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  overflow-y: auto;
  padding: 20px;
}

.funding-info-content {
  background-color: white;
  width: 800px;
  max-width: 90%;
  max-height: 90vh;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
}

.funding-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
}

.funding-header h3 {
  margin: 0;
  font-weight: 600;
  color: #343a40;
}

.funding-body {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.funding-footer {
  padding: 15px 20px;
  border-top: 1px solid #e9ecef;
  text-align: right;
}

.funding-source {
  margin-bottom: 25px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.funding-source:last-child {
  border-bottom: none;
}

.funding-source h4 {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  font-weight: 600;
  color: #343a40;
}

.funding-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  margin-right: 10px;
  font-size: 18px;
  color: white;
}

.private-pay {
  background-color: #28a745;
}

.regional-center {
  background-color: #007bff;
}

.school-based {
  background-color: #fd7e14;
}

.insurance {
  background-color: #6f42c1;
}

.insurance-list {
  display: flex;
  flex-wrap: wrap;
  padding-left: 20px;
}

.insurance-list li {
  width: 50%;
  margin-bottom: 5px;
}

.funding-note {
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 5px;
  font-size: 0.9rem;
}

/* Mobile styles */
@media (max-width: 768px) {
  .funding-info-content {
    width: 100%;
    max-width: 100%;
    height: 100%;
    max-height: 100%;
    border-radius: 0;
  }
  
  .funding-header, .funding-body, .funding-footer {
    padding: 15px;
  }
  
  .insurance-list li {
    width: 100%;
  }
}
</style>

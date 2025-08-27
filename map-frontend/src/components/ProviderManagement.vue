<template>
  <div class="provider-management">
    <div class="header">
      <h2>Provider Management</h2>
      <button @click="showCreateForm = true" class="btn btn-primary">
        <i class="fas fa-plus"></i> Add New Provider
      </button>
    </div>

    <!-- Provider List -->
    <div class="provider-list">
      <div class="search-bar">
        <input
          v-model="searchQuery"
          @input="filterProviders"
          placeholder="Search providers..."
          class="search-input"
        />
        <button @click="refreshProviders" class="btn btn-secondary">
          <i class="fas fa-refresh"></i> Refresh
        </button>
      </div>

      <div class="table-container">
        <table class="provider-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Type</th>
              <th>Phone</th>
              <th>Email</th>
              <th>Address</th>
              <th>City</th>
              <th>State</th>
              <th>ZIP</th>
              <th>Description</th>
              <th>Insurance Accepted</th>
              <th>Languages</th>
              <th>Website</th>
              <th>Latitude</th>
              <th>Longitude</th>
              <th>Verified</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="provider in filteredProviders" :key="provider.id">
              <td>{{ provider.name }}</td>
              <td>{{ provider.type || "N/A" }}</td>
              <td>{{ provider.phone || "N/A" }}</td>
              <td>{{ provider.email || "N/A" }}</td>
              <td>{{ formatAddress(provider.address) }}</td>
              <td>{{ addressPart(provider.address, 'city') || provider.city || "" }}</td>
              <td>{{ addressPart(provider.address, 'state') || provider.state || "" }}</td>
              <td>{{ addressPart(provider.address, 'zip') || addressPart(provider.address, 'zip_code') || provider.zip_code || "" }}</td>
              <td>{{ provider.description || "N/A" }}</td>
              <td>{{ formatDelimited(provider.insurance_accepted) }}</td>
              <td>{{ formatDelimited(provider.languages_spoken) }}</td>
              <td>
                <a v-if="provider.website" :href="provider.website" target="_blank" rel="noopener">
                  {{ shortUrl(provider.website) }}
                </a>
                <span v-else>N/A</span>
              </td>
              <td>{{ formatCoord(provider.latitude) }}</td>
              <td>{{ formatCoord(provider.longitude) }}</td>
              <td>
                <span v-if="provider.verified" title="Verified">✓</span>
                <span v-else title="Not verified">—</span>
              </td>
              <td>
                <button
                  class="btn btn-sm btn-outline-primary me-1"
                  @click="editProvider(provider)"
                >
                  Edit
                </button>
                <button
                  class="btn btn-sm btn-outline-danger"
                  @click="deleteProvider(provider.id)"
                >
                  Delete
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Create/Edit Form Modal -->
    <div v-if="showCreateForm || showEditForm" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ showEditForm ? "Edit Provider" : "Add New Provider" }}</h3>
          <button @click="closeModal" class="close-btn">&times;</button>
        </div>

        <form @submit.prevent="submitForm" class="provider-form">
          <div class="form-row">
            <div class="form-group">
              <label for="name">Provider Name *</label>
              <input
                id="name"
                v-model="formData.name"
                type="text"
                required
                placeholder="Enter provider name"
              />
            </div>
            <div class="mb-3">
              <label for="phone">Phone</label>
              <input
                type="tel"
                class="form-control"
                id="phone"
                v-model="formData.phone"
                placeholder="Phone number"
              />
            </div>

            <div class="mb-3">
              <label for="email">Email</label>
              <input
                type="email"
                class="form-control"
                id="email"
                v-model="formData.email"
                placeholder="Email address"
              />
            </div>

            <div class="mb-3">
              <label for="website">Website</label>
              <input
                type="url"
                class="form-control"
                id="website"
                v-model="formData.website"
                placeholder="Website URL"
              />
            </div>
          </div>

          <div class="form-group">
            <label for="address">Address</label>
            <textarea
              id="address"
              v-model="formData.address"
              placeholder="Enter full address"
              rows="2"
            ></textarea>
            <div class="address-tools">
              <button
                type="button"
                class="btn btn-secondary"
                @click="geocodePreview"
                :disabled="previewLoading"
              >
                {{ previewLoading ? "Geocoding..." : "Geocode Address" }}
              </button>
            </div>
            <div v-if="previewError" class="error-message" style="margin-top: 10px">
              {{ previewError }}
            </div>
            <div v-if="previewLat !== null && previewLng !== null" class="map-preview">
              <div class="coords">
                Lat: {{ Number(previewLat).toFixed(6) }}, Lng:
                {{ Number(previewLng).toFixed(6) }}
              </div>
              <img
                v-if="previewMapUrl && !imgError"
                :src="previewMapUrl"
                alt="Map preview"
                @error="imgError = true"
              />
              <div v-else class="map-fallback">
                <iframe
                  v-if="embedMapUrl"
                  :src="embedMapUrl"
                  width="100%"
                  height="200"
                  style="border: 0; border-radius: 4px"
                  loading="lazy"
                  referrerpolicy="no-referrer-when-downgrade"
                ></iframe>
                <a v-else :href="googleMapsLink" target="_blank" rel="noopener"
                  >Open in Google Maps</a
                >
              </div>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="latitude">Latitude</label>
              <input
                id="latitude"
                v-model="formData.latitude"
                type="number"
                step="any"
                placeholder="e.g., 34.0522"
                title="Latitude: -90 to 90 degrees (e.g., 34.0522 for Los Angeles)"
              />
            </div>
            <div class="form-group">
              <label for="longitude">Longitude</label>
              <input
                id="longitude"
                v-model="formData.longitude"
                type="number"
                step="any"
                placeholder="e.g., -118.2437"
                title="Longitude: -180 to 180 degrees (e.g., -118.2437 for Los Angeles)"
              />
            </div>
          </div>

          <div class="form-group">
            <label for="website_domain">Website</label>
            <input
              id="website_domain"
              v-model="formData.website_domain"
              type="url"
              placeholder="https://example.com"
            />
          </div>

          <div class="form-group">
            <label for="center_based_services">Center-Based Services</label>
            <input
              id="center_based_services"
              v-model="formData.center_based_services"
              type="text"
              placeholder="e.g., Los Angeles, Van Nuys"
            />
          </div>

          <div class="form-group">
            <label for="areas">Service Areas</label>
            <input
              id="areas"
              v-model="formData.areas"
              type="text"
              placeholder="e.g., San Fernando Valley, Central LA"
            />
          </div>

          <div class="form-group">
            <label for="specializations">Specializations</label>
            <textarea
              id="specializations"
              v-model="formData.specializations"
              placeholder="e.g., Autism, ABA Therapy, Early Intervention"
              rows="2"
            ></textarea>
          </div>

          <div class="mb-3">
            <label for="description">Description</label>
            <textarea
              class="form-control"
              id="description"
              v-model="formData.description"
              rows="3"
              placeholder="Service description and areas served"
            ></textarea>
          </div>

          <div class="mb-3">
            <label for="insurance_accepted">Insurance Accepted</label>
            <input
              type="text"
              class="form-control"
              id="insurance_accepted"
              v-model="formData.insurance_accepted"
              placeholder="e.g., Medi-Cal, Aetna, Blue Cross"
            />
          </div>

          <div class="form-group">
            <label for="services">Services Offered</label>
            <textarea
              id="services"
              v-model="formData.services"
              placeholder="e.g., In-Home ABA, Center-Based Therapy, Parent Training"
              rows="2"
            ></textarea>
          </div>

          <div class="form-group">
            <label for="coverage_areas">Coverage Areas</label>
            <textarea
              id="coverage_areas"
              v-model="formData.coverage_areas"
              placeholder="e.g., Los Angeles, Orange County, Ventura County"
              rows="2"
            ></textarea>
          </div>

          <div class="form-group">
            <label for="languages_spoken">Languages Spoken</label>
            <input
              type="text"
              class="form-control"
              id="languages_spoken"
              v-model="formData.languages_spoken"
              placeholder="e.g., English, Spanish, Mandarin"
            />
          </div>

          <div class="form-actions">
            <button type="button" @click="clearForm" class="btn btn-outline">
              Clear Form
            </button>
            <button type="button" @click="closeModal" class="btn btn-secondary">
              Cancel
            </button>
            <button type="submit" class="btn btn-primary">
              {{ showEditForm ? "Update Provider" : "Create Provider" }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Loading and Error States -->
    <div v-if="loading" class="loading">
      <i class="fas fa-spinner fa-spin"></i> Loading...
    </div>

    <div v-if="error" class="error-message">
      {{ error }}
    </div>

    <div v-if="successMessage" class="success-message">
      {{ successMessage }}
    </div>
  </div>
</template>

<script>
const API_BASE = (import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8000").replace(
  /\/+$/,
  ""
);
const API_ROOT = API_BASE.endsWith("/api") ? API_BASE : `${API_BASE}/api`;
export default {
  name: "ProviderManagement",
  data() {
    return {
      providers: [],
      filteredProviders: [],
      searchQuery: "",
      loading: false,
      error: null,
      successMessage: null,
      showCreateForm: false,
      showEditForm: false,
      editingProvider: null,
      formData: {
        name: "",
        phone: "",
        email: "",
        website: "",
        description: "",
        address: "",
        latitude: "",
        longitude: "",
        insurance_accepted: "",
        languages_spoken: "",
      },
      previewLat: null,
      previewLng: null,
      previewLoading: false,
      previewError: null,
      imgError: false,
    };
  },
  computed: {
    previewMapUrl() {
      const lat = this.previewLat ?? (this.formData.latitude || null);
      const lng = this.previewLng ?? (this.formData.longitude || null);
      if (lat === null || lng === null) return null;
      const token =
        import.meta.env.VITE_MAPBOX_TOKEN ||
        import.meta.env.VITE_MAPBOX_ACCESS_TOKEN ||
        import.meta.env.VITE_MAPBOX_API_TOKEN;
      const z = 14;
      const width = 600;
      const height = 220;
      if (!token) {
        // Fallback to OSM static map if no Mapbox token
        return `https://staticmap.openstreetmap.de/staticmap.php?center=${lat},${lng}&zoom=${z}&size=${width}x${height}&markers=${lat},${lng},red-pushpin`;
      }
      // Red pin + centered static image (Mapbox)
      return `https://api.mapbox.com/styles/v1/mapbox/streets-v11/static/pin-s+ff0000(${lng},${lat}),${lng},${lat},${z}/${width}x${height}?access_token=${token}`;
    },
    googleMapsLink() {
      const lat = this.previewLat ?? (this.formData.latitude || null);
      const lng = this.previewLng ?? (this.formData.longitude || null);
      if (lat === null || lng === null) return "#";
      return `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`;
    },
    embedMapUrl() {
      const lat = this.previewLat ?? (this.formData.latitude || null);
      const lng = this.previewLng ?? (this.formData.longitude || null);
      if (lat === null || lng === null) return null;
      // Google Maps embed without API key
      return `https://www.google.com/maps?q=${lat},${lng}&z=14&output=embed`;
    },
  },

  async mounted() {
    await this.loadProviders();
  },

  methods: {
    formatAddress(value) {
      if (!value) return "N/A";
      try {
        // Accept JSON string like {"street":..., "city":..., "state":..., "zip":...}
        if (typeof value === "string" && value.trim().startsWith("{")) {
          const obj = JSON.parse(value);
          const street = obj.street || obj.address || "";
          const city = obj.city || "";
          const state = obj.state || "";
          const zip = obj.zip || obj.zip_code || "";
          const parts = [street, [city, state].filter(Boolean).join(", "), zip]
            .filter((p) => (typeof p === "string" ? p.trim() : p))
            .map((p) => (typeof p === "string" ? p.trim() : p));
          const result = parts.filter(Boolean).join(", ");
          return result || value;
        }
      } catch (e) {
        // fall through to raw value
      }
      return value;
    },
    formatDelimited(value) {
      if (!value) return "N/A";
      try {
        if (typeof value === "string" && value.trim().startsWith("[")) {
          const arr = JSON.parse(value);
          if (Array.isArray(arr)) {
            return arr
              .map((v) => String(v).trim())
              .filter((v) => v.length > 0)
              .join(", ");
          }
        }
      } catch (e) {}
      // Fallback: treat as comma- or semicolon-separated string
      return String(value)
        .split(/[;,]/)
        .map((v) => v.trim())
        .filter((v) => v.length > 0)
        .join(", ");
    },
    addressPart(value, key) {
      try {
        if (typeof value === "string" && value.trim().startsWith("{")) {
          const obj = JSON.parse(value);
          return obj[key] || "";
        }
      } catch {}
      return "";
    },
    shortUrl(url) {
      try {
        const u = new URL(url);
        return u.hostname.replace(/^www\./, "") + (u.pathname !== "/" ? u.pathname : "");
      } catch {
        return url;
      }
    },
    formatCoord(v) {
      if (v === null || v === undefined || v === "") return "";
      const n = Number(v);
      if (Number.isNaN(n)) return String(v);
      return n.toFixed(6);
    },
    async geocodePreview() {
      this.previewError = null;
      this.previewLoading = true;
      try {
        const addr = (this.formData.address || "").trim();
        if (!addr) {
          this.previewError = "Please enter an address to geocode.";
          return;
        }
        const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(
          addr
        )}`;
        const res = await fetch(url, { headers: { Accept: "application/json" } });
        if (!res.ok) {
          throw new Error(`Geocoder error ${res.status}`);
        }
        const data = await res.json();
        if (!data || data.length === 0) {
          this.previewError = "No results found for that address.";
          return;
        }
        const { lat, lon } = data[0];
        this.previewLat = parseFloat(lat);
        this.previewLng = parseFloat(lon);
        // Also populate form coords for convenience
        this.formData.latitude = this.previewLat;
        this.formData.longitude = this.previewLng;
      } catch (e) {
        this.previewError = e.message || "Failed to geocode address.";
      } finally {
        this.previewLoading = false;
      }
    },
    async loadProviders() {
      this.loading = true;
      this.error = null;

      try {
        const response = await fetch(`${API_ROOT}/providers-v2/`);
        if (!response.ok) {
          const errorText = await response.text();
          throw new Error(
            `HTTP error! status: ${response.status}, response: ${errorText}`
          );
        }
        const data = await response.json();
        this.providers = data.results || data;
        this.filteredProviders = [...this.providers];
      } catch (err) {
        this.error = `Failed to load providers: ${err.message}`;
        console.error("Error loading providers:", err);
      } finally {
        this.loading = false;
      }
    },

    filterProviders() {
      if (!this.searchQuery.trim()) {
        this.filteredProviders = [...this.providers];
        return;
      }

      const query = this.searchQuery.toLowerCase();
      this.filteredProviders = this.providers.filter(
        (provider) =>
          provider.name.toLowerCase().includes(query) ||
          (provider.address && provider.address.toLowerCase().includes(query)) ||
          (provider.description && provider.description.toLowerCase().includes(query)) ||
          (provider.phone && provider.phone.includes(query))
      );
    },

    async refreshProviders() {
      await this.loadProviders();
    },

    editProvider(provider) {
      this.editingProvider = { ...provider };
      this.formData = {
        name: provider.name || "",
        phone: provider.phone || "",
        email: provider.email || "",
        website: provider.website || "",
        description: provider.description || "",
        address: provider.address || "",
        latitude: provider.latitude || "",
        longitude: provider.longitude || "",
        insurance_accepted: provider.insurance_accepted || "",
        languages_spoken: provider.languages_spoken || "",
      };
      this.showEditForm = true;
      this.error = null;
    },

    async deleteProvider(providerId) {
      if (
        !confirm(
          "Are you sure you want to delete this provider? This action cannot be undone."
        )
      ) {
        return;
      }

      try {
        const response = await fetch(`${API_ROOT}/providers-v2/${providerId}/`, {
          method: "DELETE",
          headers: {
            "Content-Type": "application/json",
          },
        });

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        // Remove from local list
        this.providers = this.providers.filter((p) => p.id !== providerId);
        this.filterProviders();

        // Show success message
        this.successMessage = "Provider deleted successfully!";
        setTimeout(() => {
          this.successMessage = null;
        }, 3000);
      } catch (err) {
        this.error = `Failed to delete provider: ${err.message}`;
        console.error("Error deleting provider:", err);
      }
    },

    async submitForm() {
      // Validate form data
      if (!this.formData.name.trim()) {
        this.error = "Provider name is required";
        return;
      }

      // Validate coordinates if provided
      if (this.formData.latitude || this.formData.longitude) {
        const lat = parseFloat(this.formData.latitude);
        const lng = parseFloat(this.formData.longitude);

        if (isNaN(lat) || isNaN(lng)) {
          this.error = "Invalid coordinates. Please enter valid numbers.";
          return;
        }

        if (lat < -90 || lat > 90) {
          this.error = "Latitude must be between -90 and 90 degrees.";
          return;
        }

        if (lng < -180 || lng > 180) {
          this.error = "Longitude must be between -180 and 180 degrees.";
          return;
        }
      }

      try {
        const url = this.showEditForm
          ? `${API_ROOT}/providers-v2/${this.editingProvider.id}/`
          : `${API_ROOT}/providers-v2/`;

        const method = this.showEditForm ? "PUT" : "POST";

        // Ensure numeric strings don't exceed backend precision
        const payload = { ...this.formData };
        if (payload.latitude !== "" && payload.latitude !== null) {
          payload.latitude = Number(
            payload.latitude?.toString()?.match(/^-?\d+(?:\.\d{0,6})?/)[0]
          );
        }
        if (payload.longitude !== "" && payload.longitude !== null) {
          payload.longitude = Number(
            payload.longitude?.toString()?.match(/^-?\d+(?:\.\d{0,6})?/)[0]
          );
        }

        const response = await fetch(url, {
          method,
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(payload),
        });

        if (!response.ok) {
          const errorData = await response.text();
          console.error("API Error Response:", errorData);
          throw new Error(
            `HTTP error! status: ${response.status}, response: ${errorData}`
          );
        }

        const result = await response.json();

        if (this.showEditForm) {
          // Update existing provider in list
          const index = this.providers.findIndex((p) => p.id === this.editingProvider.id);
          if (index !== -1) {
            this.providers[index] = result;
          }
        } else {
          // Add new provider to list
          this.providers.unshift(result);
        }

        this.filterProviders();
        this.closeModal();

        // Show success message
        this.successMessage = `Provider ${
          this.showEditForm ? "updated" : "created"
        } successfully!`;
        setTimeout(() => {
          this.successMessage = null;
        }, 3000);
      } catch (err) {
        this.error = `Failed to ${this.showEditForm ? "update" : "create"} provider: ${
          err.message
        }`;
        console.error("Error submitting form:", err);
      }
    },

    clearForm() {
      this.formData = {
        name: "",
        phone: "",
        email: "",
        website: "",
        description: "",
        address: "",
        latitude: "",
        longitude: "",
        insurance_accepted: "",
        languages_spoken: "",
      };
      this.error = null;
      this.successMessage = null;
    },

    closeModal() {
      this.showCreateForm = false;
      this.showEditForm = false;
      this.editingProvider = null;
      this.formData = {
        name: "",
        phone: "",
        email: "",
        website: "",
        description: "",
        address: "",
        latitude: "",
        longitude: "",
        insurance_accepted: "",
        languages_spoken: "",
      };
      this.error = null;
      this.successMessage = null;
      this.previewLat = null;
      this.previewLng = null;
      this.previewError = null;
    },
  },
};
</script>

<style scoped>
.provider-management {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.header h2 {
  margin: 0;
  color: #333;
}

.search-bar {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.search-input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
}

.table-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.provider-table {
  width: 100%;
  border-collapse: collapse;
}

.provider-table th,
.provider-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.provider-table th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #495057;
}

.provider-table tr:hover {
  background-color: #f8f9fa;
}

.actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-primary {
  background-color: #007bff;
  color: white;
}

.btn-primary:hover {
  background-color: #0056b3;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background-color: #545b62;
}

.btn-warning {
  background-color: #ffc107;
  color: #212529;
}

.btn-warning:hover {
  background-color: #e0a800;
}

.btn-danger {
  background-color: #dc3545;
  color: white;
}

.btn-danger:hover {
  background-color: #c82333;
}

.btn-outline {
  background-color: transparent;
  color: #007bff;
  border: 1px solid #007bff;
}

.btn-outline:hover {
  background-color: #007bff;
  color: white;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  max-width: 600px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
}

.close-btn:hover {
  color: #333;
}

.provider-form {
  padding: 20px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 600;
  color: #333;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  font-family: inherit;
}

.address-tools {
  margin-top: 8px;
}

.map-preview {
  margin-top: 12px;
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 8px;
  background: #fafafa;
}
.map-preview img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 4px;
}
.map-preview .coords {
  font-size: 12px;
  color: #555;
  margin-bottom: 6px;
}

.form-group textarea {
  resize: vertical;
  min-height: 60px;
}

.form-actions {
  display: flex;
  gap: 15px;
  justify-content: flex-end;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #666;
  font-size: 18px;
}

.error-message {
  background-color: #f8d7da;
  color: #721c24;
  padding: 15px;
  border-radius: 4px;
  margin: 20px 0;
  border: 1px solid #f5c6cb;
}

.success-message {
  background-color: #d4edda;
  color: #155724;
  padding: 15px;
  border-radius: 4px;
  margin: 20px 0;
  border: 1px solid #c3e6cb;
}

@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
  }

  .header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }

  .search-bar {
    flex-direction: column;
  }

  .table-container {
    overflow-x: auto;
  }
}
</style>

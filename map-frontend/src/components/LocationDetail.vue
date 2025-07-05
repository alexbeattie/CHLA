<template>
  <div class="info-popup">
    <div class="popup-header">
      <h5>{{ location.name || location.regional_center || "Unnamed Location" }}</h5>
      <button @click="$emit('close')" class="close-btn">×</button>
    </div>

    <div class="popup-content">
      <div class="address">
        {{ location.address || "" }}{{ location.city ? ", " + location.city : ""
        }}{{ location.state ? ", " + location.state : ""
        }}{{ location.zip_code ? " " + location.zip_code : "" }}
      </div>

      <div v-if="location.distance" class="distance">
        {{ location.distance.toFixed(1) }} miles away
      </div>

      <div v-if="location.phone || location.telephone" class="phone">
        {{ location.phone || location.telephone }}
      </div>
    </div>

    <div class="popup-actions">
      <a
        :href="`https://www.google.com/maps/dir/?api=1&destination=${location.latitude},${location.longitude}`"
        class="btn-directions"
        target="_blank"
      >
        Directions
      </a>
      <a
        v-if="location.phone || location.telephone"
        :href="`tel:${location.phone || location.telephone}`"
        class="btn-call"
      >
        Call
      </a>
    </div>
  </div>
</template>

<script>
export default {
  name: "LocationDetail",

  props: {
    location: {
      type: Object,
      required: true,
    },
  },
};
</script>

<style scoped>
.info-popup {
  position: absolute;
  top: 20px;
  right: 20px;
  background: white;
  border: 1px solid #ccc;
  border-radius: 6px;
  width: 280px;
  max-width: 90%;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  z-index: 500;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #eee;
  background: #f8f9fa;
  border-radius: 6px 6px 0 0;
}

.popup-header h5 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  flex: 1;
  padding-right: 10px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #666;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #333;
  background: #eee;
  border-radius: 50%;
}

.popup-content {
  padding: 12px;
}

.address {
  font-size: 14px;
  color: #555;
  margin-bottom: 8px;
  line-height: 1.4;
}

.distance {
  font-size: 13px;
  color: #007bff;
  font-weight: 500;
  margin-bottom: 6px;
}

.phone {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
}

.popup-actions {
  display: flex;
  gap: 8px;
  padding: 0 12px 12px 12px;
}

.btn-directions,
.btn-call {
  flex: 1;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  text-align: center;
  cursor: pointer;
  border: 1px solid;
}

.btn-directions {
  background: #007bff;
  color: white;
  border-color: #007bff;
}

.btn-directions:hover {
  background: #0056b3;
  border-color: #0056b3;
  color: white;
  text-decoration: none;
}

.btn-call {
  background: #28a745;
  color: white;
  border-color: #28a745;
}

.btn-call:hover {
  background: #1e7e34;
  border-color: #1e7e34;
  color: white;
  text-decoration: none;
}

/* Mobile responsive */
@media (max-width: 768px) {
  .info-popup {
    width: calc(100% - 20px);
    right: 10px;
    left: 10px;
    top: 10px;
    max-width: none;
  }

  .popup-actions {
    flex-direction: column;
  }
}
</style>

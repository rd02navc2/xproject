# xproject
流程:設備 → MqttServer(Moquette) → MqttServerDeviceGateway → DeviceGatewayHelper → DeviceMessageConnector/EventBus → (下行 send)

本機執行:
  backend:  cd backend && mvn spring-boot:run     (HTTP 8080 / MQTT 1883)
  frontend: cd frontend && npm i && npm run dev   (http://localhost:5173)
Docker:     docker compose up -d --build          (http://localhost)

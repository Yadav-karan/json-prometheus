**json-prometheus project setup**
1) Clone this project
2) Get the data.json from src/main/resource and place it whereever you want
<img width="182" alt="image" src="https://github.com/user-attachments/assets/870d4f33-a68b-4395-b7d8-f1ba45478f2b" />

3) Copy the path of the data.json file
4) Paste the copied path in property 'data.json.file' in application.properties file
<img width="504" alt="image" src="https://github.com/user-attachments/assets/084790c1-3b0c-4d20-bcc5-8d448c5fcde6" />

**Prometheus Setup**
1) Download prometheus and update the prometheus.yml add the below configuration in yml

   scrape_configs:
  - job_name: 'spring_boot_app'
    static_configs:
      - targets: ['localhost:8181']  # Replace with your Spring Boot app's host and port


Once done try hitiing the various endpoints
http://localhost:8181/with-feature-file/sample-metrics
<img width="959" alt="image" src="https://github.com/user-attachments/assets/74dc218f-9457-472c-93cd-f4d5159fdd26" />



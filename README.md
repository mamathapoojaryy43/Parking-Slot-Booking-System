# AeroPark - Future-Ready Smart Parking System

## 🚀 Live Demo

🔗 https://parking-slot-booking-system-01lm.onrender.com

AeroPark is an intelligent, automated parking management system built with Spring Boot. It optimizes space utilization, eliminates congestion, and automates payments with dynamic pricing and AI-driven slot recommendations.

## Features
- **AI-Powered Slot Recommendation**: Suggests the best parking slot based on vehicle type (2-wheeler, 4-wheeler, EV, Disabled Access).
- **EV Charging Integration**: Manage EV charging stations directly from the dashboard.
- **Dynamic Pricing Engine**: Hourly, daily, and monthly subscription plans.
- **Real-time Live Status**: Monitor floor-wise slot occupancy.
- **QR Confirmation & Entry**: Fast check-in via mobile-friendly QR codes.

## Tech Stack
- **Backend**: Java 17, Spring Boot, Spring Security, Spring Data JPA
- **Database**: H2 (In-Memory) / MySQL
- **Frontend**: HTML5, CSS3, JavaScript, Bootstrap 5

## Getting Started

### Prerequisites
- Java 17 installed
- Maven installed
- MySQL (Optional, if switching from H2)

### Running Locally

1. Clone the repository:
   ```bash
   git clone https://github.com/mamathapoojaryy43/Parking-Slot-Booking-System.git
   ```
2. Navigate to the project directory:
   ```bash
   cd "PARKING SYSTEM"
   ```
3. Run the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```
4. Access the application in your browser:
   - **Frontend**: [http://localhost:8080](http://localhost:8080)
   - **Admin Login**: Username: `admin` | Password: `admin123`
   - **H2 Database Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:parkingsystem`, Username: `sa`)

## License
&copy; 2026 AeroPark Systems Inc. All Rights Reserved. Built for smart cities & green carbon reductions.

CREATE TABLE IF NOT EXISTS application_locks (
 id INT PRIMARY KEY
);
INSERT INTO application_locks(id) SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM application_locks WHERE id=1);
CREATE TABLE IF NOT EXISTS users (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 email VARCHAR(190) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','PASSENGER')),
 active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS trains (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 train_name VARCHAR(100) NOT NULL,
 start_station VARCHAR(100) NOT NULL,
 destination VARCHAR(100) NOT NULL,
 capacity INT NOT NULL CHECK (capacity BETWEEN 1 AND 500),
 active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS schedules (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 train_id BIGINT NOT NULL,
 departure TIMESTAMP NOT NULL,
 arrival TIMESTAMP NOT NULL,
 fare DECIMAL(10,2) NOT NULL CHECK (fare > 0),
 capacity INT NOT NULL CHECK (capacity BETWEEN 1 AND 500),
 FOREIGN KEY (train_id) REFERENCES trains(id),
 UNIQUE (train_id, departure),
 CHECK (arrival > departure)
);
CREATE TABLE IF NOT EXISTS bookings (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 schedule_id BIGINT NOT NULL,
 booking_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 status VARCHAR(20) NOT NULL CHECK (status IN ('CONFIRMED','CANCELLED')),
 FOREIGN KEY (user_id) REFERENCES users(id),
 FOREIGN KEY (schedule_id) REFERENCES schedules(id)
);
CREATE TABLE IF NOT EXISTS tickets (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 booking_id BIGINT NOT NULL UNIQUE,
 seat_number INT NOT NULL CHECK (seat_number > 0),
 price DECIMAL(10,2) NOT NULL CHECK (price > 0),
 reference VARCHAR(40) NOT NULL UNIQUE,
 FOREIGN KEY (booking_id) REFERENCES bookings(id)
);
CREATE TABLE IF NOT EXISTS seat_allocations (
 schedule_id BIGINT NOT NULL,
 seat_number INT NOT NULL,
 booking_id BIGINT NOT NULL UNIQUE,
 PRIMARY KEY (schedule_id, seat_number),
 FOREIGN KEY (schedule_id) REFERENCES schedules(id),
 FOREIGN KEY (booking_id) REFERENCES bookings(id)
);
CREATE TABLE IF NOT EXISTS payments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 booking_id BIGINT NOT NULL UNIQUE,
 amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
 payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('CARD','CASH')),
 payment_status VARCHAR(20) NOT NULL CHECK (payment_status IN ('PAID','REFUNDED')),
 FOREIGN KEY (booking_id) REFERENCES bookings(id)
);
CREATE TABLE IF NOT EXISTS payment_transactions (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 payment_id BIGINT NOT NULL,
 transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('CHARGE','REFUND')),
 amount DECIMAL(10,2) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (payment_id) REFERENCES payments(id),
 UNIQUE (payment_id, transaction_type)
);

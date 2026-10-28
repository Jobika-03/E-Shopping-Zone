# EShoppingZone Microservices Platform

EShoppingZone is a full-featured e-commerce platform built with **Spring Boot**, **Spring MVC**, **MySQL**, and **Thymeleaf** (Pure HTML5 & CSS, zero React/JS).

## Architecture Services:
1. **discovery-service** (Port: `8761`) - Eureka Service Registry
2. **profile-service** (Port: `8081`) - User Profile, Roles & Address Management (`eshopping_profile_db`)
3. **product-service** (Port: `8082`) - Product Catalog, Categories, Specs, Ratings & Reviews (`eshopping_product_db`)
4. **cart-service** (Port: `8083`) - Shopping Cart & Item Calculations (`eshopping_cart_db`)
5. **order-service** (Port: `8084`) - Order Lifecycle, Multi-Vendor Merchant Attribution & Tracking (`eshopping_order_db`)
6. **wallet-service** (Port: `8085`) - E-Wallet Balance, Deposits & Statements (`eshopping_wallet_db`)
7. **notification-service** (Port: `8086`) - Asynchronous Email Notification Service (JavaMailSender)
8. **merchant-service** (Port: `8087`) - Dedicated Merchant Service (`eshopping_merchant_db`):
   - Strict merchant authentication (`/merchant/auth/login`)
   - Manual product creation & catalog synchronization with `product-service`
   - Customer order retrieval & filtering by `merchantId`
   - Order fulfillment & status updates (`PLACED` &rarr; `CONFIRMED` &rarr; `SHIPPED` &rarr; `OUT_FOR_DELIVERY` &rarr; `DELIVERED`)
   - Merchant KPI analytics & dashboard metrics
9. **web-ui-service** (Port: `8080`) - Storefront & Merchant Portal UI using Spring MVC & Thymeleaf

## Merchant & Seller Lifecycle:
- **Merchant Login Portal** (`http://localhost:8080/merchant/login`):
  - Strictly restricted to verified merchants (`role == MERCHANT`). Customer accounts are blocked with an access denied message.
  - Pre-seeded Demo Merchant: `merchant@eshoppingzone.com` / `merchant123`.
- **Merchant Dashboard** (`http://localhost:8080/merchant/dashboard`):
  - Real-time KPI cards: Total Products, Total Orders, Total Revenue, Pending Orders.
- **Manual Product Creation** (`http://localhost:8080/merchant/addProduct`):
  - Manual creation with name, category, type, price, image URL preview, and description.
  - Created products immediately appear on the Storefront Home page (`http://localhost:8080/home`) with the "Sold by: [Merchant Name]" badge.
- **Customer Order Management** (`http://localhost:8080/merchant/orders`):
  - When users purchase products belonging to a merchant, the orders appear in the merchant's portal.
  - The merchant can update the fulfillment status (`CONFIRMED`, `SHIPPED`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`).
- **Live User Order Tracking** (`http://localhost:8080/orders`):
  - Customers track their order progress via a visual stage stepper (`Ordered` &rarr; `Confirmed` &rarr; `Shipped` &rarr; `Out for Delivery` &rarr; `Delivered`) in real-time.

## Quick Start
1. Execute `schema.sql` in MySQL (`root` / password: `Jobika@2004`).
2. Start services in order:
   - `discovery-service`
   - `profile-service`, `product-service`, `cart-service`, `order-service`, `wallet-service`, `notification-service`, `merchant-service`
   - `web-ui-service`
3. Access Storefront: `http://localhost:8080`
4. Access Merchant Portal: `http://localhost:8080/merchant/login`

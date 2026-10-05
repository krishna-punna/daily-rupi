-- Master data: Category > Sub Category > Item.
-- Rows seeded here have is_default = TRUE; anything you add yourself is FALSE.

CREATE TABLE categories (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(80) NOT NULL,
    sort_order INT         NOT NULL DEFAULT 1000,
    is_default BOOLEAN     NOT NULL DEFAULT FALSE,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE sub_categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT      NOT NULL,
    name        VARCHAR(80) NOT NULL,
    is_default  BOOLEAN     NOT NULL DEFAULT FALSE,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_sub_categories_name UNIQUE (category_id, name),
    CONSTRAINT fk_sub_categories_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sub_category_id BIGINT      NOT NULL,
    name            VARCHAR(80) NOT NULL,
    is_default      BOOLEAN     NOT NULL DEFAULT FALSE,
    active          BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_items_name UNIQUE (sub_category_id, name),
    CONSTRAINT fk_items_sub_category FOREIGN KEY (sub_category_id) REFERENCES sub_categories (id)
);

INSERT INTO categories (name, sort_order, is_default) VALUES
    ('Housing', 10, TRUE),
    ('Utilities & Bills', 20, TRUE),
    ('Household & Groceries', 30, TRUE),
    ('Food & Dining', 40, TRUE),
    ('Transport', 50, TRUE),
    ('Loans & EMIs', 60, TRUE),
    ('Savings & Investments', 70, TRUE),
    ('Insurance', 80, TRUE),
    ('Children & Education', 90, TRUE),
    ('Health & Medical', 100, TRUE),
    ('Personal Care & Clothing', 110, TRUE),
    ('Entertainment & Leisure', 120, TRUE),
    ('Family & Social', 130, TRUE),
    ('Electronics & Gadgets', 140, TRUE),
    ('Taxes & Fees', 150, TRUE),
    ('Pets', 160, TRUE),
    ('Miscellaneous', 170, TRUE);

-- Housing
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Rent & Maintenance' AS name UNION ALL SELECT 'Home Repairs & Upkeep' AS name UNION ALL SELECT 'Furnishing & Appliances' AS name UNION ALL SELECT 'Domestic Help' AS name) v ON 1 = 1
WHERE c.name = 'Housing';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'House Rent' AS name UNION ALL SELECT 'Society Maintenance' AS name UNION ALL SELECT 'Property Tax' AS name UNION ALL SELECT 'Rental Deposit' AS name UNION ALL SELECT 'Brokerage' AS name) v ON 1 = 1
WHERE c.name = 'Housing' AND sc.name = 'Rent & Maintenance';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Plumbing' AS name UNION ALL SELECT 'Electrical Repairs' AS name UNION ALL SELECT 'Painting' AS name UNION ALL SELECT 'Carpentry' AS name UNION ALL SELECT 'Pest Control' AS name UNION ALL SELECT 'Appliance Repair' AS name) v ON 1 = 1
WHERE c.name = 'Housing' AND sc.name = 'Home Repairs & Upkeep';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Furniture' AS name UNION ALL SELECT 'Kitchen Appliances' AS name UNION ALL SELECT 'Home Appliances' AS name UNION ALL SELECT 'Home Decor' AS name UNION ALL SELECT 'Bedding & Linen' AS name) v ON 1 = 1
WHERE c.name = 'Housing' AND sc.name = 'Furnishing & Appliances';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Maid' AS name UNION ALL SELECT 'Cook' AS name UNION ALL SELECT 'Driver' AS name UNION ALL SELECT 'Gardener' AS name UNION ALL SELECT 'Watchman' AS name UNION ALL SELECT 'Laundry & Ironing' AS name) v ON 1 = 1
WHERE c.name = 'Housing' AND sc.name = 'Domestic Help';

-- Utilities & Bills
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Electricity' AS name UNION ALL SELECT 'Water & Gas' AS name UNION ALL SELECT 'Phone & Internet' AS name UNION ALL SELECT 'TV & Subscriptions' AS name) v ON 1 = 1
WHERE c.name = 'Utilities & Bills';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Power Bill' AS name UNION ALL SELECT 'Inverter & Battery' AS name UNION ALL SELECT 'Generator Fuel' AS name) v ON 1 = 1
WHERE c.name = 'Utilities & Bills' AND sc.name = 'Electricity';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Water Bill' AS name UNION ALL SELECT 'Water Tanker' AS name UNION ALL SELECT 'Drinking Water Cans' AS name UNION ALL SELECT 'LPG Cylinder' AS name UNION ALL SELECT 'Piped Gas' AS name) v ON 1 = 1
WHERE c.name = 'Utilities & Bills' AND sc.name = 'Water & Gas';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Mobile Recharge' AS name UNION ALL SELECT 'Mobile Postpaid Bill' AS name UNION ALL SELECT 'Broadband' AS name UNION ALL SELECT 'Landline' AS name) v ON 1 = 1
WHERE c.name = 'Utilities & Bills' AND sc.name = 'Phone & Internet';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'DTH / Cable TV' AS name UNION ALL SELECT 'OTT Subscriptions' AS name UNION ALL SELECT 'Music Subscriptions' AS name UNION ALL SELECT 'Newspaper & Magazines' AS name UNION ALL SELECT 'Cloud Storage' AS name) v ON 1 = 1
WHERE c.name = 'Utilities & Bills' AND sc.name = 'TV & Subscriptions';

-- Household & Groceries
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Groceries' AS name UNION ALL SELECT 'Fresh Produce & Dairy' AS name UNION ALL SELECT 'Household Supplies' AS name) v ON 1 = 1
WHERE c.name = 'Household & Groceries';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'House Daily Expenditures' AS name UNION ALL SELECT 'Monthly Provisions' AS name UNION ALL SELECT 'Rice & Grains' AS name UNION ALL SELECT 'Pulses' AS name UNION ALL SELECT 'Cooking Oil' AS name UNION ALL SELECT 'Spices' AS name) v ON 1 = 1
WHERE c.name = 'Household & Groceries' AND sc.name = 'Groceries';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Vegetables' AS name UNION ALL SELECT 'Fruits' AS name UNION ALL SELECT 'Milk' AS name UNION ALL SELECT 'Curd & Dairy' AS name UNION ALL SELECT 'Eggs' AS name UNION ALL SELECT 'Meat & Fish' AS name) v ON 1 = 1
WHERE c.name = 'Household & Groceries' AND sc.name = 'Fresh Produce & Dairy';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Cleaning Supplies' AS name UNION ALL SELECT 'Toiletries' AS name UNION ALL SELECT 'Kitchen Supplies' AS name UNION ALL SELECT 'Pooja Items' AS name) v ON 1 = 1
WHERE c.name = 'Household & Groceries' AND sc.name = 'Household Supplies';

-- Food & Dining
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Eating Out' AS name UNION ALL SELECT 'Food Delivery' AS name UNION ALL SELECT 'Beverages & Snacks' AS name) v ON 1 = 1
WHERE c.name = 'Food & Dining';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Restaurants' AS name UNION ALL SELECT 'Street Food' AS name UNION ALL SELECT 'Cafes' AS name UNION ALL SELECT 'Office Lunch' AS name) v ON 1 = 1
WHERE c.name = 'Food & Dining' AND sc.name = 'Eating Out';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Online Food Orders' AS name UNION ALL SELECT 'Sweets & Bakery' AS name) v ON 1 = 1
WHERE c.name = 'Food & Dining' AND sc.name = 'Food Delivery';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Tea & Coffee' AS name UNION ALL SELECT 'Snacks' AS name UNION ALL SELECT 'Soft Drinks & Juices' AS name) v ON 1 = 1
WHERE c.name = 'Food & Dining' AND sc.name = 'Beverages & Snacks';

-- Transport
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Fuel' AS name UNION ALL SELECT 'Vehicle Upkeep' AS name UNION ALL SELECT 'Vehicle Charges' AS name UNION ALL SELECT 'Public & Hired Transport' AS name) v ON 1 = 1
WHERE c.name = 'Transport';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Petrol' AS name UNION ALL SELECT 'Diesel' AS name UNION ALL SELECT 'CNG' AS name UNION ALL SELECT 'EV Charging' AS name) v ON 1 = 1
WHERE c.name = 'Transport' AND sc.name = 'Fuel';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Vehicle Service' AS name UNION ALL SELECT 'Repairs & Spares' AS name UNION ALL SELECT 'Tyres & Battery' AS name UNION ALL SELECT 'Car Wash' AS name UNION ALL SELECT 'Pollution Certificate' AS name) v ON 1 = 1
WHERE c.name = 'Transport' AND sc.name = 'Vehicle Upkeep';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Vehicle Insurance' AS name UNION ALL SELECT 'Road Tax' AS name UNION ALL SELECT 'Parking' AS name UNION ALL SELECT 'Tolls & FASTag' AS name UNION ALL SELECT 'Traffic Fines' AS name) v ON 1 = 1
WHERE c.name = 'Transport' AND sc.name = 'Vehicle Charges';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Bus' AS name UNION ALL SELECT 'Metro' AS name UNION ALL SELECT 'Train' AS name UNION ALL SELECT 'Auto Rickshaw' AS name UNION ALL SELECT 'Cab / Taxi' AS name UNION ALL SELECT 'Bike Taxi' AS name) v ON 1 = 1
WHERE c.name = 'Transport' AND sc.name = 'Public & Hired Transport';

-- Loans & EMIs
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Home Loan' AS name UNION ALL SELECT 'Vehicle Loan' AS name UNION ALL SELECT 'Personal & Other Loans' AS name UNION ALL SELECT 'Credit Cards' AS name) v ON 1 = 1
WHERE c.name = 'Loans & EMIs';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'House Loan EMI' AS name UNION ALL SELECT 'Home Loan Prepayment' AS name UNION ALL SELECT 'Top-up Loan EMI' AS name) v ON 1 = 1
WHERE c.name = 'Loans & EMIs' AND sc.name = 'Home Loan';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Car EMI' AS name UNION ALL SELECT 'Two-wheeler EMI' AS name) v ON 1 = 1
WHERE c.name = 'Loans & EMIs' AND sc.name = 'Vehicle Loan';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Personal Loan EMI' AS name UNION ALL SELECT 'Education Loan EMI' AS name UNION ALL SELECT 'Gold Loan Interest' AS name UNION ALL SELECT 'Consumer Durable EMI' AS name UNION ALL SELECT 'Hand Loan Repayment' AS name) v ON 1 = 1
WHERE c.name = 'Loans & EMIs' AND sc.name = 'Personal & Other Loans';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Credit Card Bill' AS name UNION ALL SELECT 'Credit Card EMI' AS name UNION ALL SELECT 'Card Annual Fee' AS name UNION ALL SELECT 'Late Fee & Interest' AS name) v ON 1 = 1
WHERE c.name = 'Loans & EMIs' AND sc.name = 'Credit Cards';

-- Savings & Investments
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Government Schemes' AS name UNION ALL SELECT 'Bank Deposits' AS name UNION ALL SELECT 'Chits' AS name UNION ALL SELECT 'Market Investments' AS name UNION ALL SELECT 'Gold & Property' AS name) v ON 1 = 1
WHERE c.name = 'Savings & Investments';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'PPF' AS name UNION ALL SELECT 'SSY (Sukanya Samriddhi)' AS name UNION ALL SELECT 'APY (Atal Pension Yojana)' AS name UNION ALL SELECT 'NPS' AS name UNION ALL SELECT 'EPF / VPF' AS name UNION ALL SELECT 'NSC' AS name UNION ALL SELECT 'Senior Citizen Savings Scheme' AS name UNION ALL SELECT 'Post Office Schemes' AS name) v ON 1 = 1
WHERE c.name = 'Savings & Investments' AND sc.name = 'Government Schemes';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'RD (Recurring Deposit)' AS name UNION ALL SELECT 'FD (Fixed Deposit)' AS name) v ON 1 = 1
WHERE c.name = 'Savings & Investments' AND sc.name = 'Bank Deposits';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Chit Fund' AS name UNION ALL SELECT 'Local Chit' AS name) v ON 1 = 1
WHERE c.name = 'Savings & Investments' AND sc.name = 'Chits';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Mutual Fund SIP' AS name UNION ALL SELECT 'Stocks' AS name UNION ALL SELECT 'ETFs' AS name UNION ALL SELECT 'Bonds' AS name UNION ALL SELECT 'Sovereign Gold Bond' AS name) v ON 1 = 1
WHERE c.name = 'Savings & Investments' AND sc.name = 'Market Investments';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Gold Purchase' AS name UNION ALL SELECT 'Gold Savings Scheme' AS name UNION ALL SELECT 'Silver' AS name UNION ALL SELECT 'Land / Plot Instalment' AS name) v ON 1 = 1
WHERE c.name = 'Savings & Investments' AND sc.name = 'Gold & Property';

-- Insurance
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Life Insurance' AS name UNION ALL SELECT 'Health Insurance' AS name UNION ALL SELECT 'General Insurance' AS name) v ON 1 = 1
WHERE c.name = 'Insurance';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'LIC Premium' AS name UNION ALL SELECT 'Term Insurance' AS name UNION ALL SELECT 'ULIP' AS name) v ON 1 = 1
WHERE c.name = 'Insurance' AND sc.name = 'Life Insurance';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Family Health Insurance' AS name UNION ALL SELECT 'Parents Health Insurance' AS name UNION ALL SELECT 'Top-up Health Plan' AS name UNION ALL SELECT 'Personal Accident Cover' AS name) v ON 1 = 1
WHERE c.name = 'Insurance' AND sc.name = 'Health Insurance';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Home Insurance' AS name UNION ALL SELECT 'Travel Insurance' AS name UNION ALL SELECT 'Gadget Insurance' AS name) v ON 1 = 1
WHERE c.name = 'Insurance' AND sc.name = 'General Insurance';

-- Children & Education
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'School' AS name UNION ALL SELECT 'Tuition & Coaching' AS name UNION ALL SELECT 'Activities' AS name UNION ALL SELECT 'Child Care' AS name UNION ALL SELECT 'Higher Education' AS name) v ON 1 = 1
WHERE c.name = 'Children & Education';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'School Fees' AS name UNION ALL SELECT 'School Bus' AS name UNION ALL SELECT 'Uniforms' AS name UNION ALL SELECT 'Books & Stationery' AS name UNION ALL SELECT 'Exam Fees' AS name UNION ALL SELECT 'School Events & Trips' AS name) v ON 1 = 1
WHERE c.name = 'Children & Education' AND sc.name = 'School';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Tuition' AS name UNION ALL SELECT 'Coaching Classes' AS name UNION ALL SELECT 'Online Courses' AS name) v ON 1 = 1
WHERE c.name = 'Children & Education' AND sc.name = 'Tuition & Coaching';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Sports Coaching' AS name UNION ALL SELECT 'Music & Dance Classes' AS name UNION ALL SELECT 'Hobby Classes' AS name UNION ALL SELECT 'Summer Camp' AS name) v ON 1 = 1
WHERE c.name = 'Children & Education' AND sc.name = 'Activities';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Daycare' AS name UNION ALL SELECT 'Babysitter' AS name UNION ALL SELECT 'Toys & Games' AS name UNION ALL SELECT 'Baby Supplies' AS name UNION ALL SELECT 'Pocket Money' AS name) v ON 1 = 1
WHERE c.name = 'Children & Education' AND sc.name = 'Child Care';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'College Fees' AS name UNION ALL SELECT 'Hostel Fees' AS name UNION ALL SELECT 'Entrance Exam Fees' AS name) v ON 1 = 1
WHERE c.name = 'Children & Education' AND sc.name = 'Higher Education';

-- Health & Medical
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Doctor & Hospital' AS name UNION ALL SELECT 'Medicines & Tests' AS name UNION ALL SELECT 'Fitness & Wellness' AS name) v ON 1 = 1
WHERE c.name = 'Health & Medical';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Doctor Consultation' AS name UNION ALL SELECT 'Hospitalisation' AS name UNION ALL SELECT 'Dental' AS name UNION ALL SELECT 'Eye Care' AS name UNION ALL SELECT 'Physiotherapy' AS name) v ON 1 = 1
WHERE c.name = 'Health & Medical' AND sc.name = 'Doctor & Hospital';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Medicines' AS name UNION ALL SELECT 'Lab Tests' AS name UNION ALL SELECT 'Health Check-up' AS name UNION ALL SELECT 'Vaccinations' AS name) v ON 1 = 1
WHERE c.name = 'Health & Medical' AND sc.name = 'Medicines & Tests';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Gym Membership' AS name UNION ALL SELECT 'Yoga Classes' AS name UNION ALL SELECT 'Sports Equipment' AS name UNION ALL SELECT 'Supplements' AS name) v ON 1 = 1
WHERE c.name = 'Health & Medical' AND sc.name = 'Fitness & Wellness';

-- Personal Care & Clothing
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Clothing' AS name UNION ALL SELECT 'Grooming' AS name UNION ALL SELECT 'Accessories' AS name) v ON 1 = 1
WHERE c.name = 'Personal Care & Clothing';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Clothes' AS name UNION ALL SELECT 'Footwear' AS name UNION ALL SELECT 'Tailoring' AS name UNION ALL SELECT 'Ethnic & Festive Wear' AS name) v ON 1 = 1
WHERE c.name = 'Personal Care & Clothing' AND sc.name = 'Clothing';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Salon / Haircut' AS name UNION ALL SELECT 'Beauty Parlour' AS name UNION ALL SELECT 'Cosmetics' AS name UNION ALL SELECT 'Spa' AS name) v ON 1 = 1
WHERE c.name = 'Personal Care & Clothing' AND sc.name = 'Grooming';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Jewellery' AS name UNION ALL SELECT 'Watches' AS name UNION ALL SELECT 'Bags' AS name UNION ALL SELECT 'Eyewear' AS name) v ON 1 = 1
WHERE c.name = 'Personal Care & Clothing' AND sc.name = 'Accessories';

-- Entertainment & Leisure
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Outings' AS name UNION ALL SELECT 'Hobbies' AS name UNION ALL SELECT 'Travel & Vacations' AS name) v ON 1 = 1
WHERE c.name = 'Entertainment & Leisure';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Movies' AS name UNION ALL SELECT 'Events & Concerts' AS name UNION ALL SELECT 'Amusement Parks' AS name UNION ALL SELECT 'Clubs & Recreation' AS name) v ON 1 = 1
WHERE c.name = 'Entertainment & Leisure' AND sc.name = 'Outings';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Books' AS name UNION ALL SELECT 'Gaming' AS name UNION ALL SELECT 'Music Instruments' AS name UNION ALL SELECT 'Photography' AS name UNION ALL SELECT 'Gardening' AS name) v ON 1 = 1
WHERE c.name = 'Entertainment & Leisure' AND sc.name = 'Hobbies';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Flights' AS name UNION ALL SELECT 'Train Tickets' AS name UNION ALL SELECT 'Bus Tickets' AS name UNION ALL SELECT 'Hotels' AS name UNION ALL SELECT 'Local Sightseeing' AS name UNION ALL SELECT 'Visa & Passport' AS name) v ON 1 = 1
WHERE c.name = 'Entertainment & Leisure' AND sc.name = 'Travel & Vacations';

-- Family & Social
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Festivals & Functions' AS name UNION ALL SELECT 'Gifts & Donations' AS name UNION ALL SELECT 'Family Support' AS name) v ON 1 = 1
WHERE c.name = 'Family & Social';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Festival Shopping' AS name UNION ALL SELECT 'Crackers & Decorations' AS name UNION ALL SELECT 'Family Functions' AS name UNION ALL SELECT 'Birthday & Anniversary' AS name) v ON 1 = 1
WHERE c.name = 'Family & Social' AND sc.name = 'Festivals & Functions';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Gifts' AS name UNION ALL SELECT 'Wedding Gifts' AS name UNION ALL SELECT 'Temple Offerings' AS name UNION ALL SELECT 'Charity & Donations' AS name) v ON 1 = 1
WHERE c.name = 'Family & Social' AND sc.name = 'Gifts & Donations';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Parents Support' AS name UNION ALL SELECT 'Relatives Support' AS name UNION ALL SELECT 'Dependants Allowance' AS name) v ON 1 = 1
WHERE c.name = 'Family & Social' AND sc.name = 'Family Support';

-- Electronics & Gadgets
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Devices' AS name UNION ALL SELECT 'Software & Services' AS name) v ON 1 = 1
WHERE c.name = 'Electronics & Gadgets';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Mobile Phone' AS name UNION ALL SELECT 'Laptop / Computer' AS name UNION ALL SELECT 'Tablet' AS name UNION ALL SELECT 'Device Accessories' AS name) v ON 1 = 1
WHERE c.name = 'Electronics & Gadgets' AND sc.name = 'Devices';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Software Subscriptions' AS name UNION ALL SELECT 'App Purchases' AS name UNION ALL SELECT 'Device Repair' AS name) v ON 1 = 1
WHERE c.name = 'Electronics & Gadgets' AND sc.name = 'Software & Services';

-- Taxes & Fees
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Taxes' AS name UNION ALL SELECT 'Fees & Charges' AS name) v ON 1 = 1
WHERE c.name = 'Taxes & Fees';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Income Tax' AS name UNION ALL SELECT 'Advance Tax' AS name UNION ALL SELECT 'Professional Tax' AS name) v ON 1 = 1
WHERE c.name = 'Taxes & Fees' AND sc.name = 'Taxes';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Bank Charges' AS name UNION ALL SELECT 'Legal Fees' AS name UNION ALL SELECT 'CA / Tax Filing Fees' AS name UNION ALL SELECT 'Government Fees' AS name UNION ALL SELECT 'Document Charges' AS name) v ON 1 = 1
WHERE c.name = 'Taxes & Fees' AND sc.name = 'Fees & Charges';

-- Pets
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Pet Care' AS name) v ON 1 = 1
WHERE c.name = 'Pets';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Pet Food' AS name UNION ALL SELECT 'Vet & Vaccines' AS name UNION ALL SELECT 'Pet Grooming' AS name UNION ALL SELECT 'Pet Supplies' AS name) v ON 1 = 1
WHERE c.name = 'Pets' AND sc.name = 'Pet Care';

-- Miscellaneous
INSERT INTO sub_categories (category_id, name, is_default)
SELECT c.id, v.name, TRUE FROM categories c JOIN (SELECT 'Unplanned' AS name UNION ALL SELECT 'Other' AS name) v ON 1 = 1
WHERE c.name = 'Miscellaneous';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Unseen Charges' AS name UNION ALL SELECT 'Emergency Expense' AS name UNION ALL SELECT 'Lost / Damaged Items' AS name) v ON 1 = 1
WHERE c.name = 'Miscellaneous' AND sc.name = 'Unplanned';

INSERT INTO items (sub_category_id, name, is_default)
SELECT sc.id, v.name, TRUE FROM sub_categories sc JOIN categories c ON c.id = sc.category_id JOIN (SELECT 'Other Expense' AS name UNION ALL SELECT 'Cash Withdrawal' AS name UNION ALL SELECT 'Tips' AS name) v ON 1 = 1
WHERE c.name = 'Miscellaneous' AND sc.name = 'Other';

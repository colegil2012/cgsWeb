const dbName = 'cgsweb';
const db = db.getSiblingDB(dbName);

print(`--- Refreshing B2C E-commerce database for: ${dbName} ---`);

// 1. Purge existing data
db.users.drop();
db.vendors.drop();
db.products.drop();
db.categories.drop();
db.orders.drop();
db.carts.drop();

db.createCollection("users");
db.createCollection("vendors");
db.createCollection("products");
db.createCollection("categories");
db.createCollection("orders");
db.createCollection("carts");

// 2. Seed Categories (Enables better filtering/navigation)
const categories = [ 
    { name: "Produce", slug: "produce" },
    { name: "Pantry", slug: "pantry" },
	{ name: "Bakery & Sweets", slug: "bakery-sweets" },
	{ name: "Beverages", slug: "beverages" },
	{ name: "Health & Wellness", slug: "health-wellness" },
    { name: "Home Decor", slug: "home-decor" },
	{ name: "Garden & Outdoor", slug: "garden-outdoor"}
];
db.categories.insertMany(categories);

const produceCat = db.categories.findOne({slug: "produce"})._id;
const pantryCat = db.categories.findOne({slug: "pantry"})._id;
const bakeryCat = db.categories.findOne({slug: "bakery-sweets"})._id;
const bevCat = db.categories.findOne({slug: "beverages"})._id;
const healthCat = db.categories.findOne({slug: "health-wellness"})._id;
const homeDecCat = db.categories.findOne({slug: "home-decor"})._id;
const gardenCat = db.categories.findOne({slug: "garden-outdoor"})._id;

// 3. Seed Vendors
const celtechResult = db.vendors.insertOne({ 
    name: 'Celtech General Store',
    slug: 'celtech-gs',
    description: 'Celtech GS was founded with 2 main goals: creating an online marketplace where you can trust the products because they\'re made by your friends and neighbors. \nSecondly, making affordable locally sourced goods that you can trust more widely available. \nAll goods sold by Celtech GS directly are either organically garden grown or locally sourced.',
    addresses: [
		{
			street: '4800 State Hwy 1066',
			city: 'Bloomfield',
			state: 'KY',
			zip: '40008', 
			isDefault: true
		}
	],
	lead_time: 3,
    logo_url: '/images/vendors/cgs.jpg',
    active: true,
    createdAt: new Date()
});
const newAgeResult = db.vendors.insertOne({ 
    name: 'New Age Craftery',
    slug: 'na-craftery',
    description: 'Hand crafted home decor, using locally sourced or recycled products.',
    addresses: [
		{
			street: '5404 Sunset Dr',
			city: 'Louisville',
			state: 'KY',
			zip: '40219', 
			isDefault: true
		}
	],
	lead_time: 3,
    logo_url: '/images/vendors/nac.jpg',
    active: true,
    createdAt: new Date()
});
const organicResult = db.vendors.insertOne({ 
    name: 'Organic Greens Co.',
    slug: 'org-greens-co',
    description: 'Garden grown greens and fruits.',
    addresses: [
		{
			street: '1006 Corn Island Ct',
			city: 'Saint Matthews',
			state: 'KY',
			zip: '40207', 
			isDefault: true
		}
	],
	lead_time: 3,
    logo_url: '/images/vendors/ogc.jpg',
    active: true,
    createdAt: new Date()
});
const nattyResult = db.vendors.insertOne({ 
    name: 'Natty Powders',
    slug: 'natty-powders',
    description: 'Locally and organically sourced ingredients, ground into premium health supplements.',
    addresses: [
		{
			street: '149 Wood Gate Dr',
			city: 'Mount Washington',
			state: 'KY',
			zip: '40047', 
			isDefault: true
		}
	],
	lead_time: 3,
    logo_url: '/images/vendors/np.jpg',
    active: true,
    createdAt: new Date()
});
const munsfordResult = db.vendors.insertOne({ 
    name: 'Munsford Farms',
    slug: 'muns-farms',
    description: 'Just a lil farm',
    addresses: [
		{
			street: '1259 Stovall Rd',
			city: 'Elizabethtown',
			state: 'KY',
			zip: '42701', 
			isDefault: true
		}
	],
	lead_time: 3,
    logo_url: '/images/vendors/mf.png',
    active: true,
    createdAt: new Date()
});



//Capture vendor ID for product and user insert
const celtechId = celtechResult.insertedId;
const newAgeId = newAgeResult.insertedId;
const organicId = organicResult.insertedId;
const nattyId = nattyResult.insertedId;
const munsfordId = munsfordResult.insertedId;

//Product table requires valid vendor ObjectId for VendorId
db.runCommand({
  collMod: "products",
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["vendorId"],
      properties: {
        vendorId: { bsonType: "objectId" }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
});

// 4. Seed Products with E-commerce metadata
db.products.insertMany([
    {
        name: "Grapes",
        slug: "organic-grapes-cgs",
        sku: "PROD-GRP-001",
        description: "Freshly picked organic grapes.",
        price: 8.99,
        salePrice: 6.99, 
        categoryId: produceCat,
        vendorId: celtechId,
        stock: 100,
        lowStockThreshold: 10,
        imageUrl: "/images/products/grapes.jpg",
        active: true,
        attributes: { length: 10, width: 8, height: 7, weight: 0.5 }, 
		readyTime: 1,
        createdAt: new Date()
    },
    {
        name: "Aleppo Salsa",
        slug: "aleppo-salsa-cgs",
        sku: "PANT-SLS-001",
        description: "Tomoatoes, aleppo and banana peppers combine for a sweet salsa.",
        price: 7.99,
        salePrice: 6.99, 
        categoryId: pantryCat,
        vendorId: celtechId,
        stock: 50,
        lowStockThreshold: 15,
        imageUrl: "/images/products/alpo_slsa.jpg",
        active: true,
        attributes: { length: 3, width: 3, height: 5, weight: 0.5 }, 
		readyTime: 1,
        createdAt: new Date()
    },
	{
        name: "Aleppo Chili Spice",
        slug: "aleppo-seasoning-cgs",
        sku: "PANT-SSN-001",
        description: "Dried Aleppo peppers and other spices mixed for a chili seasoning.",
        price: 7.99,
        salePrice: 6.99, 
        categoryId: pantryCat,
        vendorId: celtechId,
        stock: 50,
        lowStockThreshold: 15,
        imageUrl: "/images/products/alpo_spce.jpg",
        active: true,
        attributes: { length: 3, width: 3, height: 5, weight: 0.5 }, 
		readyTime: 1,
        createdAt: new Date()
    },
	{
        name: "Blueberry Jam",
        slug: "bberry-jam-cgs",
        sku: "PANT-JAM-001",
        description: "Whats the difference between jelly and jam? You can't jelly this jam in your mouth fast enough.",
        price: 9.99,
        salePrice: 7.99, 
        categoryId: pantryCat,
        vendorId: celtechId,
        stock: 100,
        lowStockThreshold: 20,
        imageUrl: "/images/products/bb_jam.jpg",
        active: true,
        attributes: { length: 3, width: 3, height: 5, weight: 0.5 }, 
		readyTime: 1,
        createdAt: new Date()
    },
    {
        name: "Chamomile Tea",
        slug: "chamomile-tea-cgs",
        sku: "BEV-TEA-001",
        description: "Soothing chamomile tea, dried and ready for brewing.",
        price: 15.99,
        categoryId: bevCat,
        vendorId: celtechId,
        stock: 15,
        lowStockThreshold: 5,
        imageUrl: "/images/products/cham_tea.jpg",
        active: true,
        attributes: { length: 4, width: 2, height: 6, weight: 0.2 },
		readyTime: 1,
        createdAt: new Date()
    },
    {
        name: "Tea Blend",
        slug: "tea-blend-cgs",
        sku: "BEV-TEA-002",
        description: "Chamomile, Lemongrass and Chalamintha mint mixed for a fragrant tea blend.",
        price: 19.99,
        categoryId: bevCat,
        vendorId: celtechId,
        stock: 50,
        lowStockThreshold: 10,
        imageUrl: "/images/products/cham_lmngrs_mnt_t.jpg",
        active: true,
        attributes: { length: 4, width: 2, height: 6, weight: 0.2 },
		readyTime: 1,
        createdAt: new Date()
    },
    {
        name: "Custom Christmas Ornament",
        slug: "xmas-ornament-nac",
        sku: "DEC-ORN-001",
        description: "Handmade Christmas ornaments, locally crafted.",
        price: 9.99,
        categoryId: homeDecCat,
        vendorId: newAgeId,
        stock: 50,
        lowStockThreshold: 10,
        imageUrl: "/images/products/crstms_orn.jpg",
        active: true,
        attributes: { length: 4, width: 4, height: 4, weight: 0.1 },
		readyTime: 1,
        createdAt: new Date()
    },
    {
        name: "Fox Ornament",
        slug: "fox-ornament-nac",
        sku: "DEC-ORN-002",
        description: "Handmade Fox themed Christmas ornaments.",
        price: 9.99,
        categoryId: homeDecCat,
        vendorId: newAgeId,
        stock: 10,
        lowStockThreshold: 2,
        imageUrl: "/images/products/fox_orn.jpg",
        active: true,
        attributes: { length: 4, width: 4, height: 4, weight: 0.1 },
		readyTime: 1,
        createdAt: new Date()
    },
    {
        name: "Handmade Candles",
        slug: "beeswax-candles-nac",
        sku: "DEC-CAN-001",
        description: "Made from locally sourced Beeswax. Natural scent and clean burn.",
        price: 7.99,
        categoryId: homeDecCat,
        vendorId: newAgeId,
        stock: 12,
        lowStockThreshold: 15,
        imageUrl: "/images/products/candles.jpg",
        active: true,
        attributes: { length: 3, width: 3, height: 4, weight: 0.8 },
		readyTime: 1,
        createdAt: new Date()
    },
    {
        name: "Organic Plant Protein Mix",
        slug: "plant-protein-np",
        sku: "HLT-PRO-001",
        description: "All natural plant based protein powder mix.",
        price: 21.99,
        categoryId: healthCat,
        vendorId: nattyId,
        stock: 150,
        lowStockThreshold: 20,
        imageUrl: "/images/products/natty_protein.jpg",
        active: true,
        attributes: { length: 6, width: 6, height: 10, weight: 2 },
		readyTime: 1,
        createdAt: new Date()
    },
    {
        name: "Ashwaganda Powder",
        slug: "ashwaganda-powder-np",
        sku: "HLT-ASH-001",
        description: "Home grown and home ground premium Ashwaganda.",
        price: 14.99,
        categoryId: healthCat,
        vendorId: nattyId,
        stock: 24,
        lowStockThreshold: 25,
        imageUrl: "/images/products/ashwa.jpg",
        active: true,
        attributes: { length: 3, width: 3, height: 5, weight: 0.5 },
		readyTime: 1,
        createdAt: new Date()
    },
    {
        name: "Kale Powder",
        slug: "kale-powder-ogc",
        sku: "HLT-KLE-001",
        description: "Premium garden grown Kale, finely ground.",
        price: 11.99,
        categoryId: healthCat,
        vendorId: organicId,
        stock: 100,
        lowStockThreshold: 10,
        imageUrl: "/images/products/kale.jpg",
        active: true,
        attributes: { length: 3, width: 3, height: 5, weight: 0.5 },
		readyTime: 1,
        createdAt: new Date()
    },
    {
        name: "Spaghetti Sauce",
        slug: "spaghetti-sauce-ogc",
        sku: "PRD-SAU-001",
        description: "Fresh tomato sauce made from garden-grown tomatoes.",
        price: 8.99,
        categoryId: pantryCat,
        vendorId: organicId,
        stock: 5,
        lowStockThreshold: 20,
        imageUrl: "/images/products/tom_sauce.jpg",
        active: true,
        attributes: { length: 4, width: 4, height: 6, weight: 1.5 },
		readyTime: 1,
        createdAt: new Date()
    },
    {
        name: "Eggs",
        slug: "farm-eggs-ogc",
        sku: "PRD-EGG-001",
        description: "Fresh farm eggs by the dozen.",
        price: 4.99,
        categoryId: produceCat,
        vendorId: organicId,
        stock: 50,
        lowStockThreshold: 10,
        imageUrl: "/images/products/eggs.jpg",
        active: true,
        attributes: { length: 12, width: 4, height: 3, weight: 1.5 },
		readyTime: 1,
        createdAt: new Date()
    },
	{
        name: "Sourdough Bread",
        slug: "farm-srdh-msf",
        sku: "BKY-BRD-001",
        description: "Fresh baked sourdough bread, actually, do you bake it? Wait, who made this?",
        price: 5.99,
        categoryId: bakeryCat,
        vendorId: munsfordId,
        stock: 100,
        lowStockThreshold: 10,
        imageUrl: "/images/products/srdgh.jpg",
        active: true,
        attributes: { length: 12, width: 4, height: 7, weight: 1.5 },
		readyTime: 2,
        createdAt: new Date()
    }, 
	{
        name: "Half Cow",
        slug: "farm-cow-msf",
        sku: "PRD-MET-001",
        description: "She put up a fight",
        price: 499.99,
        categoryId: produceCat,
        vendorId: munsfordId,
        stock: 20,
        lowStockThreshold: 5,
        imageUrl: "/images/products/half_cow.jpg",
        active: true,
        attributes: { length: 36, width: 30, height: 12, weight: 300 },
		readyTime: 1,
        createdAt: new Date()
    }
]);

// 5. Seed Users with Profiles
db.users.insertMany([
    {
        username: "cole",
        password: "$2a$10$kZRhUYzWt3mFwrM1G23yb.RTZg8V7.6nfGgJwTmRpYAhLNEsT8GSm",
		email: "colegil2012@gmail.com",
        role: "VENDOR",
		vendorId: celtechId,
        profile: {
            firstName: "Cole",
            lastName: "Gilbert",
            phone: "539-9605"
        },
        addresses: [
            {
                type: "SHIPPING",
                street: "218 Bluebill Ct.",
                city: "Shepherdsville",
                state: "KY",
                zip: "40165",
                isDefault: true
            }
        ],
        createdAt: new Date()
    },
    {
        username: "daria",
        password: "$2a$10$E0FbX/so9PA6HV6GtUtuMuqenNSVeWAyxNqOne6uG8mBOr1PcYxVa",
		email: "dariaerin@yahoo.com",
        role: "VENDOR",
		vendorId: newAgeId,
        profile: {
            firstName: "Daria",
            lastName: "Reynolds",
            phone: "619-9759"
        },
        addresses: [
            {
                type: "SHIPPING",
                street: "218 Bluebill Ct.",
                city: "Shepherdsville",
                state: "KY",
                zip: "40165",
                isDefault: true
            }
        ],
        createdAt: new Date()
    },
    {
        username: "carter",
        password: "$2a$10$sDZZipX8t3zxt3UgU5mfv.2e0dPUgb0Bdhfu7.zN4YAOVLM8XPKTW",
		email: "carterlee@gmail.com",
        role: "VENDOR",
		vendorId: nattyId,
        profile: {
            firstName: "Carter",
            lastName: "Gilbert",
            phone: "539-9605"
        },
        addresses: [
            {
                type: "SHIPPING",
                street: "218 Bluebill Ct.",
                city: "Shepherdsville",
                state: "KY",
                zip: "40165",
                isDefault: true
            }
        ],
        createdAt: new Date()
    },
    {
        username: "brynlee",
        password: "$2a$10$BPqLTQ36zuBpjLn6cLD.1./tjJOE7wa3uUGAf9SDYhDgOxjE6K6Wm",
		email: "brynleeliz@gmail.com",
        role: "VENDOR",
		vendorId: organicId,
        profile: {
            firstName: "Brynlee",
            lastName: "Gilbert",
            phone: "539-9605"
        },
        addresses: [
            {
                type: "SHIPPING",
                street: "218 Bluebill Ct.",
                city: "Shepherdsville",
                state: "KY",
                zip: "40165",
                isDefault: true
            }
        ],
        createdAt: new Date()
    },
    {
        username: "test_user",
        password: "$2a$10$Jsgmn6I2iZwM5TvR9L4ShO3Btu6J7mw2.QoblXR91JTZW3S72xU56",
		email: "test@gmail.com",
        role: "USER",
        profile: {
            firstName: "Test",
            lastName: "User",
            phone: "111-1111"
        },
        addresses: [
            {
                type: "SHIPPING",
                street: "135 Bigwood Ct.",
                city: "Louisville",
                state: "KY",
                zip: "40229",
                isDefault: true
            }
        ],
        createdAt: new Date()
    },
]);

const coleId = db.users.findOne({username: "cole"})._id;
const dariaId = db.users.findOne({username: "daria"})._id;
const carterId = db.users.findOne({username: "carter"})._id;
const brynleeId = db.users.findOne({username: "brynlee"})._id;
const userId = db.users.findOne({username: "test_user"})._id;

// 6. Seed an Example Order (The "B2C" requirement)
db.orders.insertOne({
    orderNumber: "ORD-2023-1001",
    userId: userId,
    status: "PENDING", // PENDING, PAID, SHIPPED, DELIVERED, CANCELLED
    items: [
        {
            productId: db.products.findOne({name: "Grapes"})._id,
            name: "Grapes",
            priceAtPurchase: 10.99,
            quantity: 2
        }
    ],
    totals: {
        subtotal: 21.98,
        tax: 1.32,
        shipping: 5.00,
        total: 28.30
    },
    shippingAddress: {
        street: "123 Main St",
        city: "Louisville",
        state: "KY",
        zip: "40202"
    },
    paymentStatus: "UNPAID",
    createdAt: new Date()
});

db.orders.insertOne({
    orderNumber: "ORD-2023-1001",
    userId: coleId,
    status: "PENDING", // PENDING, PAID, SHIPPED, DELIVERED, CANCELLED
    items: [
        {
            productId: db.products.findOne({name: "Grapes"})._id,
            name: "Grapes",
            priceAtPurchase: 10.99,
            quantity: 2
        }
    ],
    totals: {
        subtotal: 21.98,
        tax: 1.32,
        shipping: 5.00,
        total: 28.30
    },
    shippingAddress: {
        street: "123 Main St",
        city: "Louisville",
        state: "KY",
        zip: "40202"
    },
    paymentStatus: "UNPAID",
    createdAt: new Date()
});

print('--- B2C Seed Complete! ---');
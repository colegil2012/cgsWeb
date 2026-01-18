const dbName = 'cgs_products';
const db = db.getSiblingDB(dbName);

print(`--- Refreshing database for: ${dbName} ---`);


// 1. Purge existing data
db.users.drop();
db.vendors.drop();
db.products.drop();


// 2. Seed Vendors and capture Id's
const celtechResult = db.vendors.insertOne({
	name: 'Celtech General Store',
	createdAt: new Date()
});
const newAgeResult = db.vendors.insertOne({
	name: 'New Age Craftery',
	createdAt: new Date()
});
const organicResult = db.vendors.insertOne({
	name: 'Organic Greens Co.',
	createdAt: new Date()
});
const nattyResult = db.vendors.insertOne({
	name: 'Natty Powders',
	createdAt: new Date()
});


const celtechId = celtechResult.insertedId;
const newAgeId = newAgeResult.insertedId;
const organicId = organicResult.insertedId;
const nattyId = nattyResult.insertedId;


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

// 3. Create Users, map users with Vendor Roles to their vendorId

db.users.insertMany([
	{
	  username: "admin",
	  password: "admin",
	  role: "ADMIN",
	  createdAt: new Date()
	},
	{
	  username: "daria",
	  password: "daria",
	  role: "VENDOR",
	  vendorId: newAgeId,
	  createdAt: new Date()
	},
	{
	  username: "cole",
	  password: "cole",
	  role: "VENDOR",
	  vendorId: celtechId,
	  createdAt: new Date()
	},
	{
	  username: "carter",
	  password: "carter",
	  role: "VENDOR",
	  vendorId: nattyId,
	  createdAt: new Date()
	},
	{
	  username: "brynlee",
	  password: "brynlee",
	  role: "VENDOR",
	  vendorId: organicId,
	  createdAt: new Date()
	},
	{
	  username: "test_user",
	  password: "test",
	  role: "USER",
	  createdAt: new Date()
	}
]);

// 4. Create Products, map to vendorId above

db.products.insertMany([
	{
	  name: "Grapes",
	  description: "Just some grapes, pretty cool though",
	  price: 12.99,
	  category: "PRODUCE",
	  stock: 100,
	  imageUrl: "/images/grapes.jpg",
	  vendorId: organicId,
	  createdAt: new Date()
	},
	{
	  name: "Aleppo Salsa",
	  description: "Salsa mixed with aleppo and banana peppers for flavor",
	  price: 9.99,
	  category: "SAUCE",
	  stock: 50,
	  imageUrl: "/images/alpo_slsa.jpg",
	  vendorId: celtechId,
	  createdAt: new Date()
	},
	{
	  name: "Aleppo Chili Spice",
	  description: "Dried Aleppo peppers and other seasoning mixed for a chili seasoning",
	  price: 9.99,
	  category: "SPICE",
	  stock: 50,
	  imageUrl: "/images/alpo_spce.jpg",
	  vendorId: celtechId,
	  createdAt: new Date()
	},
	{
	  name: "Blueberry Jam",
	  description: "Whats the difference between jelly and jam? You can't jelly this jam in your mouth fast enough",
	  price: 11.99,
	  category: "JAM",
	  stock: 10,
	  imageUrl: "/images/bb_jam.jpg",
	  vendorId: celtechId,
	  createdAt: new Date()
	},
	{
	  name: "Chamomile Tea",
	  description: "Soothing chamomile tea",
	  price: 15.99,
	  category: "TEA",
	  stock: 15,
	  imageUrl: "/images/cham_tea.jpg",
	  vendorId: celtechId,
	  createdAt: new Date()
	},
	{
	  name: "Tea Blend",
	  description: "Chamomile, Lemongrass and Chalamintha mint mixed for a fragrant tea blend",
	  price: 19.99,
	  category: "TEA",
	  stock: 50,
	  imageUrl: "/images/cham_lmngrs_mnt_t.jpg",
	  vendorId: celtechId,
	  createdAt: new Date()
	},
	{
	  name: "Custom Christmas Ornament",
	  description: "Handmade Christmas ornaments",
	  price: 9.99,
	  category: "DECOR",
	  stock: 50,
	  imageUrl: "/images/crstms_orn.jpg",
	  vendorId: newAgeId,
	  createdAt: new Date()
	},
	{
	  name: "Fox Ornament",
	  description: "Handmade Fox themed Christmas ornaments",
	  price: 9.99,
	  category: "DECOR",
	  stock: 10,
	  imageUrl: "/images/fox_orn.jpg",
	  vendorId: newAgeId,
	  createdAt: new Date()
	},
	{
	  name: "Handmade Candles",
	  description: "Made from locally sourced Beeswax or something, idk how you make candles man",
	  price: 7.99,
	  category: "SCENT",
	  stock: 12,
	  imageUrl: "/images/candles.jpg",
	  vendorId: newAgeId,
	  createdAt: new Date()
	},
	{
	  name: "Organic Plant Protein Mix",
	  description: "All natural plant based protein powder mix",
	  price: 21.99,
	  category: "HEALTH",
	  stock: 150,
	  imageUrl: "/images/natty_protein.jpg",
	  vendorId: nattyId,
	  createdAt: new Date()
	},
	{
	  name: "Ashwaganda Powder",
	  description: "Home grown and home ground, then sent to your home so you can snort it or whatever the fuck you want to",
	  price: 14.99,
	  category: "HEALTH",
	  stock: 24,
	  imageUrl: "/images/ashwa.jpg",
	  vendorId: nattyId,
	  createdAt: new Date()
	},
	{
	  name: "Kale Powder",
	  description: "Premium garden grown Kale, it was a shame to have to crush it all up",
	  price: 11.99,
	  category: "HEALTH",
	  stock: 100,
	  imageUrl: "/images/kale.jpg",
	  vendorId: organicId,
	  createdAt: new Date()
	},
	{
	  name: "Spaghetti Sauce",
	  description: "Mama Mia!",
	  price: 8.99,
	  category: "SAUCE",
	  stock: 70,
	  imageUrl: "/images/tom_sauce.jpg",
	  vendorId: organicId,
	  createdAt: new Date()
	},
	{
	  name: "Eggs",
	  description: "Get your eggs by the dozen here",
	  price: 4.99,
	  category: "MEAT",
	  stock: 50,
	  imageUrl: "/images/eggs.jpg",
	  vendorId: organicId,
	  createdAt: new Date()
	}
]);

print('--- Seed Complete! ---');

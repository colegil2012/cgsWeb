db.products.insertOne({
  name: "Grapes",
  description: "Just some grapes, pretty cool though",
  price: 12.99,
  category: "fruits",
  stock: 100,
  imageUrl: "/images/grapes.jpg",
  createdAt: new Date()
})

db.products.insertOne({
  name: "Aleppo Salsa",
  description: "Salsa mixed with aleppo and banana peppers for flavor",
  price: 9.99,
  category: "sauce",
  stock: 50,
  imageUrl: "/images/alpo_slsa.jpg",
  createdAt: new Date()
})

db.products.insertOne({
  name: "Blueberry Jam",
  description: "Whats the difference between jelly and jam? You can't jelly this jam in your mouth fast enough",
  price: 11.99,
  category: "jam",
  stock: 50,
  imageUrl: "/images/bb_jam.jpg",
  createdAt: new Date()
})
db.users.insertOne({
  username: "cole_admin",
  password: "admin",
  role: "ADMIN",
  createdAt: new Date()
})

db.users.insertOne({
  username: "test_user",
  password: "test",
  role: "USER",
  createdAt: new Date()
})
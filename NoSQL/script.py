import json
import random
import pymongo
import redis
from neo4j import GraphDatabase
from bson import ObjectId

# Function to generate user data
def generate_data(num_users=100):
    users = []
    for i in range(1, num_users + 1):
        user = {
            "id": i,
            "name": f"User{i}",
            "friends": []
        }
        users.append(user)

    for user in users:
        num_friends = random.randint(1, num_users // 2)
        friends = random.sample([u["id"] for u in users if u["id"] != user["id"]], num_friends)
        user["friends"] = friends

    data = {
        "users": users
    }

    return data

# Generate data and save to a JSON file
data = generate_data(100)
with open("data.json", "w") as f:
    json.dump(data, f, indent=2)

print("data.json file has been generated.")

# MongoDB setup
client = pymongo.MongoClient("mongodb://localhost:27017/")
db = client["mydatabase"]
collection = db["users"]

# Redis setup
r = redis.Redis(host='localhost', port=6379, db=0)

# Neo4j setup
uri = "bolt://localhost:7687"
username = "neo4j"
password = "password"
driver = GraphDatabase.driver(uri, auth=(username, password))

# Load data from JSON file
with open("data.json") as file:
    data = json.load(file)

# Insert data into MongoDB
collection.insert_many(data["users"])

# Functions for creating users and friendships in Neo4j
def create_user(tx, user_id, name):
    tx.run("CREATE (u:User {id: $user_id, name: $name})", user_id=user_id, name=name)

def create_friendship(tx, user_id, friend_id):
    tx.run("""
        MATCH (u1:User {id: $user_id}), (u2:User {id: $friend_id})
        MERGE (u1)-[:FRIEND]->(u2)
        """, user_id=user_id, friend_id=friend_id)

# Insert data into Neo4j
with driver.session() as session:
    for user in data["users"]:
        session.execute_write(create_user, user["id"], user["name"])
        for friend_id in user["friends"]:
            session.execute_write(create_friendship, user["id"], friend_id)

# Function to fetch and print graph data from Neo4j
def get_graph(tx):
    result = tx.run("MATCH (u:User)-[:FRIEND]-(f:User) RETURN u.name, f.name")
    for record in result:
        print(f"{record['u.name']} is friends with {record['f.name']}")

with driver.session() as session:
    session.execute_read(get_graph)

# Custom JSON Encoder for MongoDB ObjectId
class JSONEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, ObjectId):
            return str(obj)
        return super(JSONEncoder, self).default(obj)

# Function to get user data from MongoDB with Redis caching
def get_user_from_mongo(user_id):
    cache_key = f"user:{user_id}"
    cached_user = r.get(cache_key)

    if cached_user:
        return json.loads(cached_user)

    user = collection.find_one({"id": user_id})

    if user:
        r.set(cache_key, json.dumps(user, cls=JSONEncoder), ex=3600)

    return user

# Fetch and print user data
for x in range(1, 101):
    user_data = get_user_from_mongo(x)
    print(user_data)

# Function to get graph data from Neo4j with Redis caching
def get_graph_with_cache():
    cache_key = "graph:friendships"
    cached_graph = r.get(cache_key)

    if cached_graph:
        return json.loads(cached_graph)

    graph_data = []

    with driver.session() as session:
        def fetch_graph(tx):
            result = tx.run("MATCH (u:User)-[:FRIEND]->(f:User) RETURN u.name, f.name")
            for record in result:
                graph_data.append({"user": record['u.name'], "friend": record['f.name']})

        session.execute_read(fetch_graph)

    r.set(cache_key, json.dumps(graph_data), ex=3600)

    return graph_data

# Fetch and print graph data
graph_data = get_graph_with_cache()
print(graph_data)

# Functions to clear databases
def clear_redis():
    r.flushdb()
    print("Redis database has been flushed")

def clear_mongodb():
    db.drop_collection("users")
    print("MongoDB 'users' collection has been dropped")

def clear_neo4j():
    def clear_database(tx):
        tx.run("MATCH (n) DETACH DELETE n")

    with driver.session() as session:
        session.execute_write(clear_database)
    print("Neo4J database has been cleared")

# Clear all databases
clear_redis()
clear_mongodb()
clear_neo4j()

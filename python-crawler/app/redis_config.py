from redis.cluster import RedisCluster, ClusterNode

# Redis 클러스터 설정
startup_nodes = [
    ClusterNode("127.0.0.1", 7001),
    ClusterNode("127.0.0.1", 7002),
    ClusterNode("127.0.0.1", 7003),
]

# Redis 클러스터에 연결
redis_client = RedisCluster(startup_nodes=startup_nodes, decode_responses=True)

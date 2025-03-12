from redis.cluster import RedisCluster, ClusterNode

# Redis 클러스터 설정
startup_nodes = [
    ClusterNode("redis-cluster-0.redis-cluster-headless.redis.svc.cluster.local", 6379),
    ClusterNode("redis-cluster-1.redis-cluster-headless.redis.svc.cluster.local", 6379),
    ClusterNode("redis-cluster-2.redis-cluster-headless.redis.svc.cluster.local", 6379),
    ClusterNode("redis-cluster-3.redis-cluster-headless.redis.svc.cluster.local", 6379),
    ClusterNode("redis-cluster-4.redis-cluster-headless.redis.svc.cluster.local", 6379),
    ClusterNode("redis-cluster-5.redis-cluster-headless.redis.svc.cluster.local", 6379),
]

# Redis 클러스터에 연결
redis_client = RedisCluster(startup_nodes=startup_nodes, password="CTg0n49k0M", decode_responses=True)

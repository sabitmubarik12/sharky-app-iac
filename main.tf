# VPC Configuration
resource "google_compute_network" "vpc_network" {
  name                    = "sharky-vpc"
  auto_create_subnetworks = false
}

# Subnet Configuration
resource "google_compute_subnetwork" "private_subnet" {
  name                     = "private-subnet"
  ip_cidr_range            = "10.0.1.0/24"
  region                   = var.region
  network                  = google_compute_network.vpc_network.name
  private_ip_google_access = true
}

resource "google_compute_subnetwork" "public_subnet" {
  name          = "public-subnet"
  ip_cidr_range = "10.0.2.0/24"
  region        = var.region
  network       = google_compute_network.vpc_network.name
}

# NAT Router and NAT Gateway for Private Subnet
resource "google_compute_router" "nat_router" {
  name    = "nat-router"
  region  = var.region
  network = google_compute_network.vpc_network.name
}

resource "google_compute_router_nat" "nat_gateway" {
  name                               = "nat-gateway"
  router                             = google_compute_router.nat_router.name
  region                             = var.region
  nat_ip_allocate_option             = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "LIST_OF_SUBNETWORKS"
  subnetwork {
    name                    = google_compute_subnetwork.private_subnet.name
    source_ip_ranges_to_nat = ["ALL_IP_RANGES"]
  }
}

# Firewall Rules
resource "google_compute_firewall" "allow_internal" {
  name    = "allow-internal"
  network = google_compute_network.vpc_network.name

  allow {
    protocol = "tcp"
    ports    = ["0-65535"]
  }

  source_ranges = ["10.0.0.0/16"]
}

resource "google_compute_firewall" "allow_internet_access" {
  name    = "allow-internet-access"
  network = google_compute_network.vpc_network.name

  allow {
    protocol = "tcp"
    ports    = ["80", "443"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["public-access"]
}

# GKE Cluster
resource "google_container_cluster" "gke" {
  name                = var.gke_cluster_name
  location            = var.region
  network             = google_compute_network.vpc_network.name
  subnetwork          = google_compute_subnetwork.private_subnet.name
  deletion_protection = false

  # Disable default node pool creation
  remove_default_node_pool = true

  # Explicitly set initial_node_count to 1 (required by GKE)
  initial_node_count = 1

  # Enable private cluster for added security
  private_cluster_config {
    enable_private_nodes    = true
    enable_private_endpoint = false
    master_ipv4_cidr_block  = "172.16.0.16/28"
  }
}

# Private Node Pool
resource "google_container_node_pool" "private_worker_pool" {
  name       = "private-worker-pool"
  cluster    = google_container_cluster.gke.name
  location   = var.region
  node_count = 2

  node_config {
    machine_type = "e2-medium"
    preemptible  = true
    oauth_scopes = ["https://www.googleapis.com/auth/cloud-platform"]
  }
}

# Public Node Pool
resource "google_container_node_pool" "public_worker_pool" {
  name       = "public-worker-pool"
  cluster    = google_container_cluster.gke.name
  location   = var.region
  node_count = 2

  node_config {
    machine_type = "e2-medium"
    preemptible  = true
    oauth_scopes = ["https://www.googleapis.com/auth/cloud-platform"]
    tags         = ["public-access"]
  }
}

# Obtain Kubernetes credentials for kubectl
data "google_client_config" "default" {}

output "kubernetes_cluster_name" {
  value = google_container_cluster.gke.name
}

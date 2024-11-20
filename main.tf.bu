# VPC and Subnet Configuration
resource "google_compute_network" "vpc_network" {
  name = "sharky-vpc"
}

resource "google_compute_subnetwork" "subnet" {
  name          = "sharky-subnet"
  ip_cidr_range = "10.0.0.0/16"
  region        = var.region
  network       = google_compute_network.vpc_network.name
}

# Firewall rules
resource "google_compute_firewall" "allow_internal" {
  name    = "allow-internal"
  network = google_compute_network.vpc_network.name

  allow {
    protocol = "tcp"
    ports    = ["0-65535"]
  }

  source_ranges = ["10.0.0.0/16"]
}

# GKE Cluster
resource "google_container_cluster" "gke" {
  name                = var.gke_cluster_name
  location            = var.region
  network             = google_compute_network.vpc_network.name
  subnetwork          = google_compute_subnetwork.subnet.name
  deletion_protection = false

  # Disable default node pool creation
  remove_default_node_pool = true

  # Explicitly set initial_node_count to 1 (required by GKE)
  initial_node_count = 1

  # Enabling private cluster for added security
  private_cluster_config {
    enable_private_nodes    = true
    enable_private_endpoint = false
    master_ipv4_cidr_block  = "172.16.0.16/28"
  }
}

# Worker Node Pool
resource "google_container_node_pool" "worker_pool" {
  name       = "worker-pool"
  cluster    = google_container_cluster.gke.name
  location   = var.region
  node_count = 2

  node_config {
    machine_type = "e2-medium"
    preemptible  = true
    oauth_scopes = ["https://www.googleapis.com/auth/cloud-platform"]
  }
}

# Obtain Kubernetes credentials for kubectl
data "google_client_config" "default" {}

output "kubernetes_cluster_name" {
  value = google_container_cluster.gke.name
}

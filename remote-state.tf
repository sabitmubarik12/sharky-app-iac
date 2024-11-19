terraform {
  backend "gcs" {
    bucket = "sharky-tofu-state"
    prefix = "gke-cluster-state"
  }
}

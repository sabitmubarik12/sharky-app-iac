# sharky-app-iac

This repository contains Infrastructure as Code (IaC) scripts to provision and manage the infrastructure required for deploying the Sharky application. The repository uses OpenTofu for defining and provisioning infrastructure on cloud platforms.

## Repository Structure

```
.
├── modules/                # Reusable OpenTofu modules
├── environments/           # Environment-specific configurations (e.g., dev, prod)
├── scripts/                # Helper scripts for automation
├── variables.tf            # Input variable definitions
├── outputs.tf              # Output variable definitions
├── main.tf                 # Main OpenTofu configuration
└── README.md               # Project documentation
```

## Key Features

Modular Design:
Reusable modules for standardizing infrastructure components.

Environment-Specific Configurations:
Separate configurations for dev, staging, and production environments.

GitOps Friendly:
Compatible with GitOps workflows for managing IaC changes.

Security:
Secure management of sensitive data using encrypted variables.

## Prerequisites

OpenTofu CLI (v1.3 or higher)
Access to a cloud provider (e.g., AWS, GCP)
Credentials for cloud provider configuration
Backend storage for OpenTofu state (e.g., AWS S3)

## Getting Started

Clone the repository:

```
git clone https://github.com/sabitmubarik12/sharky-app-iac.git
cd sharky-app-iac
```

Initialize OpenTofu:

```
tofu init
```
Format the configuration:

```
tofu fmt
```

Validate the configuration:

```
tofu validate
```

Plan the infrastructure changes:

```
tofu plan
```

Apply the configuration:

```
tofu apply --auto-approve
```
## Notes
Replace the sample terraform.tfvars file in environments/ with your environment-specific values.
Ensure the appropriate permissions are set up for managing cloud resources.

Use OpenTofu workspaces for managing multiple environments if necessary.

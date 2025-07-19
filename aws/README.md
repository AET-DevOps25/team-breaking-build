# AWS Infrastructure

This directory contains Infrastructure as Code (IaC) configurations for deploying the Recipefy application to AWS using Terraform for infrastructure provisioning and Ansible for server configuration and application deployment.

## Overview

The AWS deployment stack consists of:
- **Terraform**: Provisions AWS infrastructure including EC2 instances and security groups
- **Ansible**: Configures the EC2 instance and deploys the containerized application stack

## Architecture

The deployment creates a single EC2 instance running the entire application stack using Docker Compose:
- **EC2 Instance**: Ubuntu 22.04 LTS (t3.large by default)
- **Security Groups**: Configured for SSH (22), HTTP (80), and application port (3000)
- **Application Stack**: Complete microservices deployment via Docker Compose

## Directory Structure

```
aws/
├── terraform/           # Infrastructure provisioning
│   ├── main.tf         # Main Terraform configuration
│   ├── variables.tf    # Input variables
│   └── outputs.tf      # Output values
└── ansible/            # Server configuration & deployment
    ├── playbook.yml    # Main Ansible playbook
    ├── hosts.ini       # Inventory file
    └── files/          # Configuration files
        ├── docker-compose.yml  # Application stack definition
        └── config/             # Application configurations
```

## Components

### Terraform Infrastructure

**Resources Provisioned:**
- AWS EC2 instance with Ubuntu 22.04 LTS
- Security group with ingress rules for SSH, HTTP, and application ports
- 50GB GP3 EBS root volume

**Key Features:**
- Configurable instance type (default: t3.large)
- Dynamic security group naming with timestamps
- Public IP assignment for internet access

### Ansible Configuration

**Automated Setup:**
- System package updates and Docker installation
- Docker Compose v2 installation
- User permissions configuration
- Application deployment via Docker Compose

**Application Stack:**
- Client (React frontend)
- API Gateway (Spring Boot)
- Recipe Service (Spring Boot)
- Version Control Service (Spring Boot)
- Keycloak Service (Authentication)
- GenAI Service (Python FastAPI)
- Databases (PostgreSQL, MongoDB)
- Vector Database (Weaviate)
- Authentication (Keycloak)

## Prerequisites

- AWS CLI configured with appropriate credentials
- Terraform >= 1.3.0
- Ansible >= 2.9
- SSH key pair configured in AWS (default: 'vockey')

## Configuration

### Terraform Variables

Configure the following variables in `terraform/variables.tf` or via command line:

```hcl
# AWS region for deployment
aws_region = "us-east-1"  # default

# EC2 instance type
instance_type = "t3.large"  # default

# AWS key pair name for SSH access
key_name = "your-key-pair-name"
```

### Environment Variables

Set up the encrypted environment file:
```bash
# Create .env_vault with your application secrets
cp ansible/files/.env_vault.example ansible/files/.env_vault
# Edit with your actual values
```

## Deployment

### 1. Infrastructure Provisioning

```bash
cd terraform

# Initialize Terraform
terraform init

# Plan the deployment
terraform plan

# Apply the configuration
terraform apply
```

### 2. Get Instance Information

```bash
# Get the public IP of the created instance
terraform output public_ip
```

### 3. Update Ansible Inventory

Update `ansible/hosts.ini` with the EC2 instance details:
```ini
[recipefy]
your-ec2-public-dns-or-ip

[recipefy:vars]
ansible_user=ubuntu
ansible_ssh_private_key_file=path/to/your/private/key.pem
```

### 4. Deploy Application

```bash
cd ../ansible

# Test connectivity
ansible all -m ping

# Deploy the application
ansible-playbook playbook.yml
```

## Application Access

After successful deployment:
- **Frontend**: `http://<EC2_PUBLIC_IP>:3000`
- **API Gateway**: `http://<EC2_PUBLIC_IP>:8080`
- **Keycloak**: `http://<EC2_PUBLIC_IP>:8080` (admin console)

## Monitoring

The deployed stack includes:
- Application health endpoints at `/actuator/health`
- Prometheus metrics at `/actuator/prometheus`
- Docker container monitoring via built-in Docker stats

## Security Considerations

- Security group restricts access to necessary ports only
- SSH access requires private key authentication
- Application uses encrypted environment variables
- Consider implementing SSL/TLS for production deployments

## Maintenance

### Updating the Application

```bash
# Re-run Ansible playbook to pull latest images
ansible-playbook playbook.yml
```

### Scaling Considerations

Current deployment is single-instance. For production scaling:
- Implement Application Load Balancer
- Use RDS for managed databases
- Consider ECS or EKS for container orchestration
- Implement auto-scaling groups

### Backup Strategy

- EC2 snapshots for system backups
- Database backups via application-specific methods
- Configuration files stored in version control

## Troubleshooting

### Common Issues

1. **SSH Connection Failed**
   - Verify key pair permissions: `chmod 400 your-key.pem`
   - Check security group SSH rules
   - Confirm correct public IP/DNS

2. **Application Not Starting**
   - Check Docker daemon status: `systemctl status docker`
   - Review application logs: `docker compose logs`
   - Verify environment variables in `.env_vault`

3. **Terraform Apply Failed**
   - Verify AWS credentials and permissions
   - Check resource limits in AWS account
   - Review Terraform state conflicts

### Log Locations

- System logs: `/var/log/`
- Docker logs: `docker compose logs -f`
- Application logs: Available via each service's logging configuration

## Cost Optimization

- Use appropriate instance types for your workload
- Consider spot instances for non-production environments
- Implement resource tagging for cost tracking
- Monitor usage via AWS Cost Explorer

## CI/CD Integration

The infrastructure can be integrated with CI/CD pipelines:
- Terraform plans in pull requests
- Automated deployments via GitHub Actions
- Infrastructure validation and security scanning

Refer to `.github/workflows/deploy_aws.yml` for the automated deployment pipeline implementation. 
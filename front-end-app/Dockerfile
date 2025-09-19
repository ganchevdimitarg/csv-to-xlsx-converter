# Dockerfile for csv-to-xlxs-converter

# Use an official Node.js runtime as a parent image
FROM node:18.2.0-alpine

# Set the working directory in the container
WORKDIR /app

# Copy package.json and package-lock.json to the working directory
COPY package*.json ./

# Install any needed packages specified in package.json
RUN npm install --production

# Bundle app source inside Docker image
COPY .. .

# Make port 3000 available to the world outside this container
EXPOSE 3000

# Define environment variable
ENV NAME World

# Run app.py when the container launches
CMD ["serve", "-s", "build"]
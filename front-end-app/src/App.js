import React, { useState, useCallback, useEffect } from 'react';
import { Upload, Download, FileText, CheckCircle, AlertCircle, X, Loader2 } from 'lucide-react';

const CSVToXLSXConverter = () => {
    const [dragActive, setDragActive] = useState(false);
    const [file, setFile] = useState(null);
    const [isConverting, setIsConverting] = useState(false);
    const [downloadUrl, setDownloadUrl] = useState('');
    const [convertedFileName, setConvertedFileName] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState(false);
    const [outputFileName, setOutputFileName] = useState('');
    const [fileList, setFileList] = useState([]); // New state for file list
    const [messages, setMessages] = useState([]); // State to track messages and their visibility

    useEffect(() => {
        const fetchFiles = async () => {
            try {
                const response = await fetch('http://localhost:8089/api/v1/files');
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                const files = await response.json();
                setFileList(files);
            } catch (err) {
                console.error('Error fetching files:', err);
            }
        };

        fetchFiles();
    }, []);

    // Function to add a message with a timeout to hide it
    const addMessage = (type, text) => {
        const messageId = Date.now(); // Unique ID for the message
        setMessages([...messages, { id: messageId, type, text, visible: true }]);
        setTimeout(() => {
            setMessages(messages.filter(msg => msg.id !== messageId));
        }, 5000);
    };

    // Handle drag events
    const handleDrag = useCallback((e) => {
        e.preventDefault();
        e.stopPropagation();
        if (e.type === "dragenter" || e.type === "dragover") {
            setDragActive(true);
        } else if (e.type === "dragleave") {
            setDragActive(false);
        }
    }, []);

    // Handle drop event
    const handleDrop = useCallback((e) => {
        e.preventDefault();
        e.stopPropagation();
        setDragActive(false);
        setError('');
        setSuccess(false);

        if (e.dataTransfer.files && e.dataTransfer.files[0]) {
            const droppedFile = e.dataTransfer.files[0];
            if (droppedFile.type === 'text/csv' || droppedFile.name.endsWith('.csv')) {
                setFile(droppedFile);
                // Auto-generate output filename
                const baseName = droppedFile.name.replace('.csv', '');
                setOutputFileName(`${baseName}_converted.xlsx`);
            } else {
                setError('Please upload a CSV file.');
                addMessage('error', 'Please upload a CSV file.');
            }
        }
    }, []);

    // Handle file input change
    const handleFileSelect = (e) => {
        setError('');
        setSuccess(false);
        if (e.target.files && e.target.files[0]) {
            const selectedFile = e.target.files[0];
            if (selectedFile.type === 'text/csv' || selectedFile.name.endsWith('.csv')) {
                setFile(selectedFile);
                // Auto-generate output filename
                const baseName = selectedFile.name.replace('.csv', '');
                setOutputFileName(`${baseName}_converted.xlsx`);
            } else {
                setError('Please select a CSV file.');
                addMessage('error', 'Please select a CSV file.');
            }
        }
    };

    // Convert CSV to XLSX
    const handleConvert = async () => {
        if (!file) {
            setError('Please select a CSV file first.');
            addMessage('error', 'Please select a CSV file first.');
            return;
        }

        if (!outputFileName.trim()) {
            setError('Please enter an output filename.');
            addMessage('error', 'Please enter an output filename.');
            return;
        }

        setIsConverting(true);
        setError('');
        setSuccess(false);
        setDownloadUrl('');

        try {
            // Create FormData for multipart/form-data request
            const formData = new FormData();
            formData.append('file', file);

            // Add output filename as query parameter
            const url = new URL('http://localhost:8089/api/v1/convert');
            url.searchParams.append('outputFileName', outputFileName);

            const response = await fetch(url.toString(), {
                method: 'POST',
                body: formData,
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
            }

            // The API returns a string (likely a file path or identifier)
            const result = await response.text();

            // Remove quotes if the response is a quoted string
            const cleanResult = result.replace(/^"(.*)"$/, '$1');

            setConvertedFileName(cleanResult);
            setSuccess(true);
            addMessage('success', 'Conversion completed successfully!');

            // If the result is a downloadable URL or file path, create download URL
            // This assumes your server provides a way to download the converted file
            // You may need to adjust this based on your server's download endpoint
            setDownloadUrl(`http://localhost:8089/api/v1/download/${outputFileName}`);

        } catch (err) {
            console.error('Conversion error:', err);
            setError(`Conversion failed: ${err.message}`);
            addMessage('error', `Conversion failed: ${err.message}`);
        } finally {
            setIsConverting(false);
        }
    };

    // Download converted file
    const handleDownload = async () => {
        if (!downloadUrl && !convertedFileName) return;

        try {
            let finalDownloadUrl = downloadUrl;

            // If no download URL was set, try to construct one
            if (!finalDownloadUrl && convertedFileName) {
                finalDownloadUrl = `http://localhost:8089/api/v1/download/${encodeURIComponent(convertedFileName)}`;
            }

            const downloadResponse = await fetch(finalDownloadUrl);

            if (!downloadResponse.ok) {
                throw new Error(`Download failed: ${downloadResponse.status}`);
            }

            const blob = await downloadResponse.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = outputFileName || convertedFileName;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);

        } catch (err) {
            console.error('Download error:', err);
            setError(`Download failed: ${err.message}`);
            addMessage('error', `Download failed: ${err.message}`);
        }
    };

    // Clear file selection and reset state
    const clearFile = () => {
        setFile(null);
        setError('');
        setSuccess(false);
        setConvertedFileName('');
        setDownloadUrl('');
        setOutputFileName('');
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 p-4">
            <div className="max-w-2xl mx-auto">
                <div className="text-center mb-8">
                    <h1 className="text-4xl font-bold text-gray-800 mb-2">
                        CSV to XLSX Converter
                    </h1>
                    <p className="text-gray-600">
                        Drag and drop your CSV file or click to select, then convert to XLSX format
                    </p>
                </div>

                <div className="bg-white rounded-xl shadow-lg p-6 space-y-6">
                    {/* Drag and Drop Area */}
                    <div
                        className={`border-2 border-dashed rounded-lg p-8 text-center transition-all duration-200 ${
                            dragActive
                                ? 'border-blue-400 bg-blue-50'
                                : file
                                    ? 'border-green-400 bg-green-50'
                                    : 'border-gray-300 bg-gray-50 hover:border-blue-400 hover:bg-blue-50'
                        }`}
                        onDragEnter={handleDrag}
                        onDragLeave={handleDrag}
                        onDragOver={handleDrag}
                        onDrop={handleDrop}
                    >
                        <input
                            type="file"
                            accept=".csv"
                            onChange={handleFileSelect}
                            className="hidden"
                            id="file-input"
                        />

                        {!file ? (
                            <label htmlFor="file-input" className="cursor-pointer block">
                                <Upload className="mx-auto h-12 w-12 text-gray-400 mb-4" />
                                <p className="text-lg font-medium text-gray-700 mb-2">
                                    Drop your CSV file here
                                </p>
                                <p className="text-sm text-gray-500">
                                    or click to select a file from your computer
                                </p>
                            </label>
                        ) : (
                            <div className="flex items-center justify-center space-x-3">
                                <FileText className="h-8 w-8 text-green-500" />
                                <div className="text-left">
                                    <p className="font-medium text-gray-700">{file.name}</p>
                                    <p className="text-sm text-gray-500">
                                        {(file.size / 1024).toFixed(1)} KB
                                    </p>
                                </div>
                                <button
                                    onClick={clearFile}
                                    className="ml-4 p-1 hover:bg-gray-200 rounded-full transition-colors"
                                    title="Remove file"
                                >
                                    <X className="h-5 w-5 text-gray-500" />
                                </button>
                            </div>
                        )}
                    </div>

                    {/* Output Filename Input */}
                    {file && (
                        <div className="space-y-2">
                            <label htmlFor="output-filename" className="block text-sm font-medium text-gray-700">
                                Output Filename
                            </label>
                            <input
                                id="output-filename"
                                type="text"
                                value={outputFileName}
                                onChange={(e) => setOutputFileName(e.target.value)}
                                placeholder="Enter output filename (e.g., converted_data.xlsx)"
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                            />
                        </div>
                    )}

                    {/* Error Message */}
                    {error && (
                        <div className="flex items-start space-x-2 p-3 bg-red-50 border border-red-200 rounded-lg">
                            <AlertCircle className="h-5 w-5 text-red-500 flex-shrink-0 mt-0.5" />
                            <p className="text-red-700 text-sm">{error}</p>
                        </div>
                    )}

                    {/* Success Message */}
                    {success && (
                        <div className="flex items-start space-x-2 p-3 bg-green-50 border border-green-200 rounded-lg">
                            <CheckCircle className="h-5 w-5 text-green-500 flex-shrink-0 mt-0.5" />
                            <div className="text-green-700 text-sm">
                                <p className="font-medium">Conversion completed successfully!</p>
                                <p className="mt-1">File: {convertedFileName}</p>
                            </div>
                        </div>
                    )}

                    {/* Action Buttons */}
                    <div className="flex space-x-4">
                        <button
                            onClick={handleConvert}
                            disabled={!file || isConverting || !outputFileName.trim()}
                            className={`flex-1 flex items-center justify-center space-x-2 py-3 px-4 rounded-lg font-medium transition-all duration-200 ${
                                !file || isConverting || !outputFileName.trim()
                                    ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                    : 'bg-blue-600 hover:bg-blue-700 text-white shadow-md hover:shadow-lg transform hover:scale-105'
                            }`}
                        >
                            {isConverting ? (
                                <>
                                    <Loader2 className="h-5 w-5 animate-spin" />
                                    <span>Converting...</span>
                                </>
                            ) : (
                                <>
                                    <FileText className="h-5 w-5" />
                                    <span>Convert to XLSX</span>
                                </>
                            )}
                        </button>

                        <button
                            onClick={handleDownload}
                            disabled={!success || (!downloadUrl && !convertedFileName)}
                            className={`flex-1 flex items-center justify-center space-x-2 py-3 px-4 rounded-lg font-medium transition-all duration-200 ${
                                !success || (!downloadUrl && !convertedFileName)
                                    ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                    : 'bg-green-600 hover:bg-green-700 text-white shadow-md hover:shadow-lg transform hover:scale-105'
                            }`}
                        >
                            <Download className="h-5 w-5" />
                            <span>Download XLSX</span>
                        </button>
                    </div>

                    {/* Message List */}
                    {messages.map((message, index) => (
                        message.visible && (
                            <div
                                key={index}
                                className={`flex items-start space-x-2 p-3 bg-${
                                    message.type === 'error' ? 'red' : 'green'
                                }-50 border border-${
                                    message.type === 'error' ? 'red' : 'green'
                                }-200 rounded-lg`}
                            >
                                <div className="flex-shrink-0">
                                    {message.type === 'error' ? (
                                        <AlertCircle className="h-5 w-5 text-red-500" />
                                    ) : (
                                        <CheckCircle className="h-5 w-5 text-green-500" />
                                    )}
                                </div>
                                <p
                                    className={`text-${
                                        message.type === 'error' ? 'red' : 'green'
                                    }-700 text-sm`}
                                >
                                    {message.text}
                                </p>
                            </div>
                        )
                    ))}

                    {/* File List */}
                    {fileList.length > 0 && (
                        <div className="mt-8 bg-white rounded-xl shadow-lg p-6 space-y-4">
                            <h3 className="font-medium text-gray-700 mb-2">Available Files</h3>
                            <ul className="list-disc list-inside">
                                {fileList.map((filename, index) => (
                                    <li key={index} className="text-gray-600">{filename}</li>
                                ))}
                            </ul>
                        </div>
                    )}

                    {/* Usage Instructions */}
                    <div className="mt-8 p-4 bg-gray-50 rounded-lg">
                        <h3 className="font-medium text-gray-800 mb-2">How to use:</h3>
                        <ol className="text-sm text-gray-600 space-y-1 list-decimal list-inside">
                            <li>Drag and drop a CSV file or click to select one</li>
                            <li>Optionally modify the output filename</li>
                            <li>Click "Convert to XLSX" to process your file</li>
                            <li>Once converted, click "Download XLSX" to get your file</li>
                        </ol>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default CSVToXLSXConverter;
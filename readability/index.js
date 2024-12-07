#!/usr/bin/env node

import { Readability } from '@mozilla/readability';
import { JSDOM } from 'jsdom';

// Buffer to store the input HTML
let inputHtml = '';

// Read from STDIN
process.stdin.setEncoding('utf8');

process.stdin.on('data', chunk => {
    inputHtml += chunk;
});

process.stdin.on('end', () => {
    try {
        // Create virtual DOM from input HTML
        const dom = new JSDOM(inputHtml, {
            url: 'https://example.com'
        });

        // Create new Readability object
        const reader = new Readability(dom.window.document);
        
        // Parse the content
        const article = reader.parse();
        
        if (article) {
            const content = dom.window.document.createElement('div');
            content.innerHTML = article.content;

            // Function to clean text
            function cleanText(text) {
                return text
                    .split('\n')
                    .map(line => line.trim())
                    .filter(line => line)  // Remove empty lines
                    .join(' ');
            }

            // Function to extract text with proper spacing
            function extractText(element) {
                let text = [];
                for (const node of element.childNodes) {
                    if (node.nodeType === 3) { // Text node
                        const trimmed = node.textContent.trim();
                        if (trimmed) text.push(cleanText(trimmed));
                    } else if (node.nodeType === 1) { // Element node
                        const childText = extractText(node);
                        if (childText) text.push(childText);
                    }
                }
                return text.join('\n');
            }

            console.log(extractText(content));
        } else {
            console.error('Could not extract text content from the provided HTML');
            process.exit(1);
        }
    } catch (error) {
        console.error('Error processing HTML:', error.message);
        process.exit(1);
    }
});

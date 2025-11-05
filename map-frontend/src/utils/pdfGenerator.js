/**
 * PDF Generator for Provider Search Results
 * Generates a formatted PDF document with provider information
 */
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';

/**
 * Generate a PDF of provider search results
 * @param {Array} providers - Array of provider objects
 * @param {Object} searchInfo - Search criteria and metadata
 */
export function generateProviderPDF(providers, searchInfo = {}) {
  const doc = new jsPDF();
  
  // Header
  doc.setFontSize(20);
  doc.setTextColor(0, 72, 119); // Brand blue
  doc.text('KINDD Provider List', 105, 20, { align: 'center' });
  
  doc.setFontSize(10);
  doc.setTextColor(100, 100, 100);
  doc.text('Los Angeles County Developmental Services', 105, 27, { align: 'center' });
  
  // Search criteria
  let yPos = 40;
  doc.setFontSize(11);
  doc.setTextColor(0, 0, 0);
  
  if (searchInfo.location) {
    doc.text(`Search Location: ${searchInfo.location}`, 15, yPos);
    yPos += 7;
  }
  
  if (searchInfo.regionalCenter) {
    doc.text(`Regional Center: ${searchInfo.regionalCenter}`, 15, yPos);
    yPos += 7;
  }
  
  if (searchInfo.filters && Object.keys(searchInfo.filters).length > 0) {
    doc.text('Filters Applied:', 15, yPos);
    yPos += 5;
    doc.setFontSize(9);
    
    if (searchInfo.filters.therapies && searchInfo.filters.therapies.length > 0) {
      doc.text(`  • Therapies: ${searchInfo.filters.therapies.join(', ')}`, 15, yPos);
      yPos += 5;
    }
    if (searchInfo.filters.insurances && searchInfo.filters.insurances.length > 0) {
      doc.text(`  • Insurance: ${searchInfo.filters.insurances.join(', ')}`, 15, yPos);
      yPos += 5;
    }
    if (searchInfo.filters.diagnoses && searchInfo.filters.diagnoses.length > 0) {
      doc.text(`  • Diagnoses: ${searchInfo.filters.diagnoses.join(', ')}`, 15, yPos);
      yPos += 5;
    }
    doc.setFontSize(11);
    yPos += 3;
  }
  
  doc.setFontSize(10);
  doc.text(`Total Providers: ${providers.length}`, 15, yPos);
  doc.text(`Generated: ${new Date().toLocaleDateString()}`, 140, yPos);
  yPos += 10;
  
  // Provider table
  const tableData = providers.map((provider, index) => {
    // Format address - handle JSON format
    let address = 'Address not available';
    if (provider.address) {
      // Check if address is JSON string
      if (provider.address.startsWith('{') || provider.address.startsWith('[')) {
        try {
          const addressObj = JSON.parse(provider.address);
          // Build address from JSON parts
          const parts = [];
          if (addressObj.street) parts.push(addressObj.street);
          if (addressObj.city) parts.push(addressObj.city);
          if (addressObj.state) parts.push(addressObj.state);
          if (addressObj.zip) parts.push(addressObj.zip);
          address = parts.join(', ');
        } catch (e) {
          // If JSON parse fails, use as-is
          address = provider.address;
        }
      } else {
        address = provider.address;
      }
      
      // Truncate if too long
      if (address.length > 45) {
        address = address.substring(0, 42) + '...';
      }
    }
    
    // Format phone
    const phone = provider.phone || 'N/A';
    
    // Format website - make it readable and clickable
    let website = 'N/A';
    if (provider.website) {
      // Clean up the website URL
      let cleanUrl = provider.website.trim();
      // Remove protocol for readability
      cleanUrl = cleanUrl.replace(/^https?:\/\//, '');
      // Remove trailing slash
      cleanUrl = cleanUrl.replace(/\/$/, '');
      // Remove www. for cleaner look
      cleanUrl = cleanUrl.replace(/^www\./, '');
      
      website = cleanUrl;
      
      // Truncate if too long
      if (website.length > 35) {
        website = website.substring(0, 32) + '...';
      }
    }
    
    return [
      provider.name || 'Unknown',
      address,
      phone,
      website
    ];
  });
  
  autoTable(doc, {
    startY: yPos,
    head: [['Provider Name', 'Address', 'Phone', 'Website']],
    body: tableData,
    styles: {
      fontSize: 9,
      cellPadding: 3,
    },
    headStyles: {
      fillColor: [0, 72, 119],
      textColor: [255, 255, 255],
      fontStyle: 'bold',
      fontSize: 10,
    },
    alternateRowStyles: {
      fillColor: [245, 245, 245],
    },
    columnStyles: {
      0: { cellWidth: 50 },  // Provider Name
      1: { cellWidth: 55 },  // Address
      2: { cellWidth: 30 },  // Phone
      3: { cellWidth: 45 },  // Website
    },
    margin: { left: 10, right: 10 },
  });
  
  // Footer on last page
  const pageCount = doc.internal.getNumberOfPages();
  for (let i = 1; i <= pageCount; i++) {
    doc.setPage(i);
    doc.setFontSize(8);
    doc.setTextColor(150, 150, 150);
    doc.text(
      `Page ${i} of ${pageCount}`,
      105,
      doc.internal.pageSize.height - 10,
      { align: 'center' }
    );
    doc.text(
      'Visit kinddhelp.com for the most up-to-date provider information',
      105,
      doc.internal.pageSize.height - 5,
      { align: 'center' }
    );
  }
  
  // Generate filename
  const date = new Date().toISOString().split('T')[0];
  let filename = `KINDD-Providers-${date}.pdf`;
  
  if (searchInfo.regionalCenter) {
    const rcShort = searchInfo.regionalCenter.replace(/\s+/g, '-').substring(0, 20);
    filename = `KINDD-${rcShort}-${date}.pdf`;
  } else if (searchInfo.location) {
    const locShort = searchInfo.location.replace(/\s+/g, '-').substring(0, 20);
    filename = `KINDD-${locShort}-${date}.pdf`;
  }
  
  // Save the PDF
  doc.save(filename);
  
  return filename;
}


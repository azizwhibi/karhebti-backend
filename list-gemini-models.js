const https = require('https');

const apiKey = 'AIzaSyC6jwlsFUcyJfV3-BGFQB3gG-xAHl8Csyk';
const url = `https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`;

https.get(url, (res) => {
  let data = '';

  res.on('data', (chunk) => {
    data += chunk;
  });

  res.on('end', () => {
    try {
      const response = JSON.parse(data);
      
      if (response.models) {
        console.log('Available Gemini models:\n');
        response.models.forEach(model => {
          console.log(`Model: ${model.name}`);
          console.log(`  Display Name: ${model.displayName || 'N/A'}`);
          if (model.supportedGenerationMethods) {
            console.log(`  Supported Methods: ${model.supportedGenerationMethods.join(', ')}`);
          }
          console.log('---');
        });
      } else {
        console.log('Response:', JSON.stringify(response, null, 2));
      }
    } catch (error) {
      console.error('Error parsing response:', error.message);
      console.log('Raw response:', data);
    }
  });
}).on('error', (error) => {
  console.error('Error fetching models:', error.message);
});


module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  moduleNameMapper: {
    '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
    '^@inductiveautomation/perspective-client$': '<rootDir>/src/__mocks__/perspective-client.ts'
  },
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts']
};

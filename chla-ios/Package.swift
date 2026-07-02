// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "CHLA-iOS",
    defaultLocalization: "en",
    platforms: [
        .iOS(.v17),
        .macOS(.v14)
    ],
    products: [
        .library(
            name: "CHLA-iOS",
            targets: ["CHLA-iOS"]
        )
    ],
    dependencies: [],
    targets: [
        .target(
            name: "CHLA-iOS",
            dependencies: [],
            path: "CHLA-iOS",
            resources: [
                .process(
                    "Resources",
                    localization: .default,
                    excluding: ["Info.plist"]
                )
            ]
        )
    ]
)


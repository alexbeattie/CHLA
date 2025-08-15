from django.core.management.base import BaseCommand
# from django.contrib.gis.geos import Polygon, MultiPolygon
from locations.models import RegionalCenter
import math


class Command(BaseCommand):
    help = "Generate realistic county-based service areas for regional centers"

    def add_arguments(self, parser):
        parser.add_argument(
            "--overwrite",
            action="store_true",
            help="Overwrite existing service areas",
        )
        parser.add_argument(
            "--dry-run",
            action="store_true",
            help="Show what would be generated without saving",
        )

    def handle(self, *args, **options):
        centers = RegionalCenter.objects.filter(
            latitude__isnull=False, longitude__isnull=False, county_served__isnull=False
        )

        if not options["overwrite"]:
            centers = centers.filter(service_area__isnull=True)

        # California county boundary approximations (simplified polygons)
        # These are approximate county shapes based on major landmarks and borders
        county_boundaries = self.get_county_boundaries()

        service_areas = {}

        self.stdout.write(f"Processing {centers.count()} regional centers...")

        for center in centers:
            try:
                county = center.county_served
                center_lat = float(center.latitude)
                center_lng = float(center.longitude)

                # Get county-specific boundary or create a default one
                if county in county_boundaries:
                    boundary_points = county_boundaries[county]
                    # Adjust the county boundary to be centered around the regional center
                    adjusted_boundary = self.adjust_boundary_to_center(
                        boundary_points, center_lat, center_lng
                    )
                else:
                    # Create a default county-sized area for unknown counties
                    adjusted_boundary = self.create_default_county_boundary(
                        center_lat, center_lng
                    )

                # Create polygon
                polygon = Polygon(adjusted_boundary)
                multi_polygon = MultiPolygon([polygon])

                service_areas[center] = multi_polygon

                if options["dry_run"]:
                    self.stdout.write(
                        f"Would generate county-based area for: {center.regional_center} "
                        f"in {county} County"
                    )
                else:
                    center.service_area = multi_polygon
                    center.save()
                    self.stdout.write(
                        self.style.SUCCESS(
                            f"✓ Generated county-based service area for {center.regional_center} "
                            f"in {county} County"
                        )
                    )

            except Exception as e:
                self.stdout.write(
                    self.style.ERROR(
                        f"Error processing {center.regional_center}: {str(e)}"
                    )
                )

        if not options["dry_run"]:
            self.stdout.write(
                self.style.SUCCESS(
                    f"\nSuccessfully generated {len(service_areas)} county-based service areas!"
                )
            )

    def get_county_boundaries(self):
        """
        Simplified county boundary coordinates for major California counties.
        These are approximate shapes based on real county borders.
        """
        return {
            # Los Angeles County - realistic shape following coastline and mountains
            "Los Angeles": [
                (-119.3175, 34.8233),
                (-118.9448, 34.8233),
                (-118.5721, 34.7740),
                (-118.2993, 34.6261),
                (-118.1265, 34.4783),
                (-117.9538, 34.3304),
                (-117.7810, 34.1825),
                (-117.6083, 34.0347),
                (-117.4355, 33.8868),
                (-117.2628, 33.7390),
                (-117.0900, 33.5911),
                (-117.1563, 33.4573),
                (-117.3289, 33.4080),
                (-117.5017, 33.3587),
                (-117.6744, 33.3094),
                (-117.8472, 33.2601),
                (-118.0199, 33.2108),
                (-118.1927, 33.1615),
                (-118.3654, 33.1122),
                (-118.5382, 33.0629),
                (-118.7109, 33.0136),
                (-118.8837, 32.9643),
                (-118.9500, 33.0629),
                (-119.0164, 33.1615),
                (-119.0827, 33.2601),
                (-119.1491, 33.3587),
                (-119.2154, 33.4573),
                (-119.2818, 33.5559),
                (-119.3482, 33.6545),
                (-119.4145, 33.7531),
                (-119.4809, 33.8517),
                (-119.5472, 33.9503),
                (-119.4809, 34.0489),
                (-119.4145, 34.1475),
                (-119.3482, 34.2461),
                (-119.2818, 34.3447),
                (-119.2154, 34.4433),
                (-119.1491, 34.5419),
                (-119.0827, 34.6405),
                (-119.0164, 34.7391),
                (-118.9500, 34.8377),
                (-119.3175, 34.8233),
            ],
            # Orange County - realistic coastal shape with Newport Bay and Santa Ana Mountains
            "Orange": [
                (-118.1265, 33.9472),
                (-118.0109, 33.9226),
                (-117.8954, 33.8980),
                (-117.7798, 33.8734),
                (-117.6643, 33.8488),
                (-117.5487, 33.8242),
                (-117.4332, 33.7996),
                (-117.4192, 33.7503),
                (-117.4052, 33.7010),
                (-117.3912, 33.6517),
                (-117.3772, 33.6024),
                (-117.3632, 33.5531),
                (-117.3492, 33.5038),
                (-117.3352, 33.4545),
                (-117.3212, 33.4052),
                (-117.3072, 33.3559),
                (-117.2932, 33.3066),
                (-117.2792, 33.2573),
                (-117.2652, 33.2080),
                (-117.3815, 33.1834),
                (-117.4977, 33.1588),
                (-117.6140, 33.1342),
                (-117.7302, 33.1096),
                (-117.8465, 33.0850),
                (-117.9627, 33.0604),
                (-118.0790, 33.0358),
                (-118.1952, 33.0112),
                (-118.2115, 33.0605),
                (-118.2277, 33.1098),
                (-118.2440, 33.1591),
                (-118.2602, 33.2084),
                (-118.2765, 33.2577),
                (-118.2927, 33.3070),
                (-118.3090, 33.3563),
                (-118.3252, 33.4056),
                (-118.3415, 33.4549),
                (-118.3577, 33.5042),
                (-118.3740, 33.5535),
                (-118.3902, 33.6028),
                (-118.4065, 33.6521),
                (-118.4227, 33.7014),
                (-118.4390, 33.7507),
                (-118.4552, 33.8000),
                (-118.4715, 33.8493),
                (-118.4877, 33.8986),
                (-118.5040, 33.9479),
                (-118.3152, 33.9476),
                (-118.1265, 33.9472),
            ],
            # San Diego County - realistic shape with coastline, mountains, and Mexico border
            "San Diego": [
                (-117.6462, 33.5587),
                (-117.5306, 33.5341),
                (-117.4151, 33.5095),
                (-117.2995, 33.4849),
                (-117.1840, 33.4603),
                (-117.0684, 33.4357),
                (-116.9529, 33.4111),
                (-116.8373, 33.3865),
                (-116.7218, 33.3619),
                (-116.6062, 33.3373),
                (-116.4907, 33.3127),
                (-116.3751, 33.2881),
                (-116.2596, 33.2635),
                (-116.1440, 33.2389),
                (-116.0285, 33.2143),
                (-115.9129, 33.1897),
                (-115.7974, 33.1651),
                (-115.6818, 33.1405),
                (-115.5663, 33.1159),
                (-115.4507, 33.0913),
                (-115.3352, 33.0667),
                (-115.2196, 33.0421),
                (-115.1041, 33.0175),
                (-114.9885, 32.9929),
                (-114.8730, 32.9683),
                (-114.7574, 32.9437),
                (-114.6419, 32.9191),
                (-114.5263, 32.8945),
                (-114.4108, 32.8699),
                (-114.2952, 32.8453),
                (-114.1797, 32.8207),
                (-114.0641, 32.7961),
                (-113.9486, 32.7715),
                (-113.8330, 32.7469),
                (-113.7175, 32.7223),
                (-113.6019, 32.6977),
                (-113.4864, 32.6731),
                (-113.3708, 32.6485),
                (-113.2553, 32.6239),
                (-113.1397, 32.5993),
                (-113.0242, 32.5747),
                (-112.9086, 32.5501),
                (-112.9249, 32.5008),
                (-112.9412, 32.4515),
                (-112.9575, 32.4022),
                (-112.9738, 32.3529),
                (-112.9901, 32.3036),
                (-113.0064, 32.2543),
                (-113.0227, 32.2050),
                (-113.0390, 32.1557),
                (-113.0553, 32.1064),
                (-113.1716, 32.1310),
                (-113.2879, 32.1556),
                (-113.4042, 32.1802),
                (-113.5205, 32.2048),
                (-113.6368, 32.2294),
                (-113.7531, 32.2540),
                (-113.8694, 32.2786),
                (-113.9857, 32.3032),
                (-114.1020, 32.3278),
                (-114.2183, 32.3524),
                (-114.3346, 32.3770),
                (-114.4509, 32.4016),
                (-114.5672, 32.4262),
                (-114.6835, 32.4508),
                (-114.7998, 32.4754),
                (-114.9161, 32.5000),
                (-115.0324, 32.5246),
                (-115.1487, 32.5492),
                (-115.2650, 32.5738),
                (-115.3813, 32.5984),
                (-115.4976, 32.6230),
                (-115.6139, 32.6476),
                (-115.7302, 32.6722),
                (-115.8465, 32.6968),
                (-115.9628, 32.7214),
                (-116.0791, 32.7460),
                (-116.1954, 32.7706),
                (-116.3117, 32.7952),
                (-116.4280, 32.8198),
                (-116.5443, 32.8444),
                (-116.6606, 32.8690),
                (-116.7769, 32.8936),
                (-116.8932, 32.9182),
                (-117.0095, 32.9428),
                (-117.1258, 32.9674),
                (-117.2421, 32.9920),
                (-117.3584, 33.0166),
                (-117.4747, 33.0412),
                (-117.5910, 33.0658),
                (-117.7073, 33.0904),
                (-117.8236, 33.1150),
                (-117.8399, 33.1643),
                (-117.8562, 33.2136),
                (-117.8725, 33.2629),
                (-117.8888, 33.3122),
                (-117.9051, 33.3615),
                (-117.9214, 33.4108),
                (-117.9377, 33.4601),
                (-117.9540, 33.5094),
                (-117.8384, 33.5340),
                (-117.7228, 33.5586),
                (-117.6072, 33.5832),
                (-117.6462, 33.5587),
            ],
            # Riverside County - large inland county
            "Riverside": [
                (-117.6462, 34.4348),
                (-115.4704, 34.4348),
                (-115.4704, 33.2680),
                (-116.9277, 33.2680),
                (-117.6462, 33.7501),
                (-117.6462, 34.4348),
            ],
            # San Bernardino County - largest county, extends far east
            "San Bernardino": [
                (-118.1265, 35.0103),
                (-114.0396, 35.0103),
                (-114.0396, 34.0429),
                (-116.3632, 34.0429),
                (-117.6462, 34.4348),
                (-118.1265, 35.0103),
            ],
            # Kern County - Central Valley
            "Kern": [
                (-119.5690, 35.8558),
                (-117.9932, 35.8558),
                (-117.9932, 34.8233),
                (-119.0261, 34.8233),
                (-119.5690, 35.4643),
                (-119.5690, 35.8558),
            ],
            # Fresno County - Central Valley
            "Fresno": [
                (-120.4245, 37.4297),
                (-118.8487, 37.4297),
                (-118.8487, 35.8558),
                (-119.5690, 35.8558),
                (-120.4245, 36.6411),
                (-120.4245, 37.4297),
            ],
            # Santa Barbara County - coastal
            "Santa Barbara": [
                (-120.6216, 34.9276),
                (-119.5147, 34.9276),
                (-119.5147, 34.3891),
                (-120.0560, 34.3891),
                (-120.6216, 34.5863),
                (-120.6216, 34.9276),
            ],
            # Ventura County - coastal
            "Ventura": [
                (-119.5147, 34.9276),
                (-118.6678, 34.9276),
                (-118.6678, 34.0429),
                (-119.3175, 34.0429),
                (-119.5147, 34.4348),
                (-119.5147, 34.9276),
            ],
            # San Francisco County - realistic peninsula shape with Golden Gate and Pacific coast
            "San Francisco": [
                (-122.5144, 37.8085),
                (-122.5051, 37.8032),
                (-122.4958, 37.7979),
                (-122.4865, 37.7926),
                (-122.4772, 37.7873),
                (-122.4679, 37.7820),
                (-122.4586, 37.7767),
                (-122.4493, 37.7714),
                (-122.4400, 37.7661),
                (-122.4307, 37.7608),
                (-122.4214, 37.7555),
                (-122.4121, 37.7502),
                (-122.4028, 37.7449),
                (-122.3935, 37.7396),
                (-122.3842, 37.7343),
                (-122.3749, 37.7290),
                (-122.3656, 37.7237),
                (-122.3563, 37.7184),
                (-122.3470, 37.7131),
                (-122.3544, 37.7071),
                (-122.3637, 37.7024),
                (-122.3730, 37.6977),
                (-122.3823, 37.6930),
                (-122.3916, 37.6883),
                (-122.4009, 37.6836),
                (-122.4102, 37.6789),
                (-122.4195, 37.6742),
                (-122.4288, 37.6695),
                (-122.4381, 37.6648),
                (-122.4474, 37.6601),
                (-122.4567, 37.6554),
                (-122.4660, 37.6507),
                (-122.4753, 37.6460),
                (-122.4846, 37.6413),
                (-122.4939, 37.6366),
                (-122.5032, 37.6319),
                (-122.5125, 37.6272),
                (-122.5218, 37.6325),
                (-122.5311, 37.6378),
                (-122.5404, 37.6431),
                (-122.5497, 37.6484),
                (-122.5590, 37.6537),
                (-122.5683, 37.6590),
                (-122.5776, 37.6643),
                (-122.5869, 37.6696),
                (-122.5962, 37.6749),
                (-122.6055, 37.6802),
                (-122.6148, 37.6855),
                (-122.6241, 37.6908),
                (-122.6334, 37.6961),
                (-122.6427, 37.7014),
                (-122.6520, 37.7067),
                (-122.6613, 37.7120),
                (-122.6706, 37.7173),
                (-122.6799, 37.7226),
                (-122.6892, 37.7279),
                (-122.6985, 37.7332),
                (-122.7078, 37.7385),
                (-122.7171, 37.7438),
                (-122.7264, 37.7491),
                (-122.7357, 37.7544),
                (-122.7450, 37.7597),
                (-122.7543, 37.7650),
                (-122.7636, 37.7703),
                (-122.7729, 37.7756),
                (-122.7822, 37.7809),
                (-122.7915, 37.7862),
                (-122.8008, 37.7915),
                (-122.8101, 37.7968),
                (-122.8194, 37.8021),
                (-122.8287, 37.8074),
                (-122.8380, 37.8127),
                (-122.8380, 37.8180),
                (-122.8287, 37.8233),
                (-122.8194, 37.8286),
                (-122.8101, 37.8339),
                (-122.8008, 37.8392),
                (-122.7915, 37.8445),
                (-122.7822, 37.8498),
                (-122.7729, 37.8551),
                (-122.7636, 37.8604),
                (-122.7543, 37.8657),
                (-122.7450, 37.8710),
                (-122.7357, 37.8763),
                (-122.7264, 37.8816),
                (-122.7171, 37.8869),
                (-122.7078, 37.8922),
                (-122.6985, 37.8975),
                (-122.6892, 37.9028),
                (-122.6799, 37.9081),
                (-122.6706, 37.9134),
                (-122.6613, 37.9187),
                (-122.6520, 37.9240),
                (-122.6427, 37.9293),
                (-122.6334, 37.9346),
                (-122.6241, 37.9399),
                (-122.6148, 37.9452),
                (-122.6055, 37.9505),
                (-122.5962, 37.9558),
                (-122.5869, 37.9611),
                (-122.5776, 37.9664),
                (-122.5683, 37.9717),
                (-122.5590, 37.9770),
                (-122.5497, 37.9823),
                (-122.5404, 37.9876),
                (-122.5311, 37.9929),
                (-122.5218, 37.9982),
                (-122.5125, 38.0035),
                (-122.5032, 38.0088),
                (-122.4939, 38.0141),
                (-122.4846, 38.0194),
                (-122.4753, 38.0247),
                (-122.4660, 38.0300),
                (-122.4567, 38.0353),
                (-122.4474, 38.0406),
                (-122.4381, 38.0459),
                (-122.4288, 38.0512),
                (-122.4195, 38.0565),
                (-122.4102, 38.0618),
                (-122.4009, 38.0671),
                (-122.3916, 38.0724),
                (-122.3823, 38.0777),
                (-122.3730, 38.0830),
                (-122.3637, 38.0883),
                (-122.3544, 38.0936),
                (-122.3544, 37.8085),
                (-122.5144, 37.8085),
            ],
            # Alameda County - Bay Area
            "Alameda": [
                (-122.3544, 37.9099),
                (-121.4690, 37.9099),
                (-121.4690, 37.4836),
                (-122.3544, 37.4836),
                (-122.3544, 37.9099),
            ],
            # Santa Clara County - Bay Area/Silicon Valley
            "Santa Clara": [
                (-122.2030, 37.4836),
                (-121.2133, 37.4836),
                (-121.2133, 36.8932),
                (-122.2030, 36.8932),
                (-122.2030, 37.4836),
            ],
            # Sacramento County - Central Valley
            "Sacramento": [
                (-121.7390, 38.7442),
                (-121.0208, 38.7442),
                (-121.0208, 38.1581),
                (-121.7390, 38.1581),
                (-121.7390, 38.7442),
            ],
            # San Joaquin County - Central Valley
            "San Joaquin": [
                (-121.8404, 38.1581),
                (-120.8507, 38.1581),
                (-120.8507, 37.5850),
                (-121.8404, 37.5850),
                (-121.8404, 38.1581),
            ],
        }

    def adjust_boundary_to_center(self, boundary_points, center_lat, center_lng):
        """
        Adjust a county boundary to be reasonably centered around the regional center.
        """
        # Calculate the centroid of the original boundary
        lats = [point[1] for point in boundary_points]
        lngs = [point[0] for point in boundary_points]
        boundary_center_lat = sum(lats) / len(lats)
        boundary_center_lng = sum(lngs) / len(lngs)

        # Calculate offset needed to center around the regional center
        lat_offset = center_lat - boundary_center_lat
        lng_offset = center_lng - boundary_center_lng

        # Apply offset but limit it to keep boundaries realistic
        # Don't move more than 0.5 degrees (about 35 miles)
        lat_offset = max(-0.5, min(0.5, lat_offset))
        lng_offset = max(-0.5, min(0.5, lng_offset))

        # Apply the offset to all boundary points
        adjusted_points = [
            (lng + lng_offset, lat + lat_offset) for lng, lat in boundary_points
        ]

        # Ensure the polygon is closed
        if adjusted_points[0] != adjusted_points[-1]:
            adjusted_points.append(adjusted_points[0])

        return adjusted_points

    def create_default_county_boundary(self, center_lat, center_lng):
        """
        Create a default county-sized boundary for counties without predefined shapes.
        """
        # Create a roughly rectangular county shape (about 40x30 miles)
        lat_size = 0.6  # About 40 miles north-south
        lng_size = 0.8  # About 50 miles east-west (adjusted for latitude)

        return [
            (center_lng - lng_size / 2, center_lat + lat_size / 2),  # Northwest
            (center_lng + lng_size / 2, center_lat + lat_size / 2),  # Northeast
            (center_lng + lng_size / 2, center_lat - lat_size / 2),  # Southeast
            (center_lng - lng_size / 2, center_lat - lat_size / 2),  # Southwest
            (center_lng - lng_size / 2, center_lat + lat_size / 2),  # Close polygon
        ]
